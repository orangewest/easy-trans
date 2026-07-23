package io.github.orangewest.trans.util;


import java.lang.annotation.Annotation;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Method;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Optional;

public class ReflectUtils {

    public static final Map<Class<?>, Class<?>> PRIMITIVE_WRAPPER_MAP = new ConcurrentHashMap<>(8);
    /**
     * Cache key: runtime class + field name to extract.
     * Same {@code R} type may be extracted by multiple target fields with different {@code key}
     * (e.g. {@code CityEntity} for both {@code name} and {@code pid}), so cache key must include key name.
     */
    private record KeyField(Class<?> clazz, String key) {
    }
    /**
     * Field accessor: getter-first to support Hibernate lazy-loading proxies (getter triggers init),
     * field access fallback for plain POJOs.
     */
    private record Accessor(Method getter, Field field) {
        Object read(Object target) throws ReflectiveOperationException {
            if (getter != null) {
                return getter.invoke(target);
            }
            return field.get(target);
        }
    }

    /**
     * Lazy cache of key accessors: KeyField -> Optional<Accessor>.
     * Optional handles "no such key" (null) caching.
     */
    private static final Map<KeyField, Optional<Accessor>> KEY_FIELD_CACHE = new ConcurrentHashMap<>();

    static {
        PRIMITIVE_WRAPPER_MAP.put(boolean.class, Boolean.class);
        PRIMITIVE_WRAPPER_MAP.put(byte.class, Byte.class);
        PRIMITIVE_WRAPPER_MAP.put(char.class, Character.class);
        PRIMITIVE_WRAPPER_MAP.put(double.class, Double.class);
        PRIMITIVE_WRAPPER_MAP.put(float.class, Float.class);
        PRIMITIVE_WRAPPER_MAP.put(int.class, Integer.class);
        PRIMITIVE_WRAPPER_MAP.put(long.class, Long.class);
        PRIMITIVE_WRAPPER_MAP.put(short.class, Short.class);
    }

    private static final System.Logger REFLECT_LOGGER = System.getLogger(ReflectUtils.class.getName());

    public static List<Field> getAllField(Class<?> clazz) {
        List<Field> result = new ArrayList<>();
        for (; clazz != null && clazz != Object.class; clazz = clazz.getSuperclass()) {
            try {
                result.addAll(Arrays.asList(clazz.getDeclaredFields()));
            } catch (Throwable t) {
                // 某父类 getDeclaredFields 抛错时记录而非静默忽略，避免漏字段难以排查
                REFLECT_LOGGER.log(System.Logger.Level.WARNING, "getAllField: getDeclaredFields failed for " + clazz, t);
            }
        }
        return result;
    }

    public static Object invokeAnnotation(Class<? extends Annotation> annotationType, Annotation annotation, String methodName) {
        try {
            return annotationType.getMethod(methodName).invoke(annotation);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 在解析阶段（每个被翻译类仅一次）从源注解反射提取属性，放入 Map。
     *
     * <p>只读取注解<b>自身声明的属性</b>（如自定义元注解 {@code @DictTrans} 的 {@code group()}），
     * <b>不</b>递归其元注解（如 {@code @TransRepo} 的 {@code name()}/{@code using()}）。
     * 元注解属性由 {@link io.github.orangewest.trans.core.TransRepoMeta} 单独处理（决定仓库名、仓库类型），
     * 若混入本 Map，既无意义，又可能因同名覆盖掉自定义注解自身的属性值。
     *
     * <p>提取后框架运行时只读取返回的 Map，不再做任何反射，从而保持 GraalVM Native Image 下的 AOT 干净。
     *
     * @param annotation 待提取的源注解实例（{@code @TransRepo} / 自定义元注解 / 等），可为 {@code null}
     * @return 属性名 -> 属性值；注解为 {@code null} 时返回空 Map
     */
    public static Map<String, Object> extractAnnotationAttributes(Annotation annotation) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (annotation == null) {
            return result;
        }
        for (Method method : annotation.annotationType().getDeclaredMethods()) {
            try {
                setAccessible(method);
                result.put(method.getName(), method.invoke(annotation));
            } catch (ReflectiveOperationException e) {
                // 个别属性反射失败则跳过，不影响其它属性
            }
        }
        return result;
    }

    /**
     * 读取字段值。调用方传入的 field 必须在解析期 / 懒缓存期已 {@link #setAccessible(AccessibleObject)}，
     * 因此此处不再重复 setAccessible（热路径去重，见 ADR-0003）。
     */
    public static Object getFieldValue(Object obj, Field field) {
        if (null == field) {
            return null;
        }
        if (obj instanceof Class) {
            obj = null;
        }
        try {
            return field.get(obj);
        } catch (IllegalAccessException e) {
            REFLECT_LOGGER.log(System.Logger.Level.WARNING, "getFieldValue failed for " + field, e);
            return null;
        }
    }

    public static <T extends AccessibleObject> void setAccessible(T accessibleObject) throws SecurityException {
        if (null != accessibleObject) {
            accessibleObject.setAccessible(true);
        }
    }

    /**
     * 写入字段值。调用方传入的 field 必须在解析期 / 懒缓存期已 {@link #setAccessible(AccessibleObject)}，
     * 因此此处不再重复 setAccessible（热路径去重，见 ADR-0003）。
     */
    public static void setFieldValue(Object obj, Field field, Object fieldValue) {
        try {
            field.set(obj, fieldValue);
        } catch (IllegalAccessException e) {
            REFLECT_LOGGER.log(System.Logger.Level.WARNING,
                    "setFieldValue failed for " + field + " on " + (obj == null ? "null" : obj.getClass()), e);
        }
    }

    /**
     * 从翻译结果值 {@code value} 中取出 {@code key} 指定的那一个子字段值。
     *
     * <p>取代 {@code beanToMap}：只为取一个字段而反射遍历整个对象所有字段。语义等价且只读取需要的字段：
     * <ul>
     *   <li>若 {@code value} 是 {@code Map}：直接 {@code map.get(key)}（保留原 {@code beanToMap} 对 Map 返回值的短路）；</li>
     *   <li>否则：按 {@code value} 的<b>实际运行期类</b>从 {@link #KEY_FIELD_CACHE} 懒取 key 字段
     *       （复刻 {@link #getAllField} 的跨父类遍历），首次遇到该类时 {@code setAccessible} 一次并缓存，随后 {@code field.get(value)}。</li>
     * </ul>
     *
     * <p>key 字段的类在解析期未知（取决于仓库返回的 {@code R}），故缓存须按运行期类维度建立。
     *
     * @param value 翻译结果对象（或 Map）
     * @param key   要提取的子字段名
     * @return key 字段值；value 为 null / key 字段不存在时返回 null
     */
    public static Object readValueByKey(Object value, String key) {
        if (value == null) {
            return null;
        }
        if (value instanceof Map<?, ?> map) {
            return map.get(key);
        }
        Accessor accessor = KEY_FIELD_CACHE.computeIfAbsent(new KeyField(value.getClass(), key), k -> Optional.ofNullable(findAccessor(k.clazz(), k.key())))
                .orElse(null);
        if (accessor == null) {
            return null;
        }
        try {
            return accessor.read(value);
        } catch (ReflectiveOperationException e) {
            return null;
        }
    }

    /**
     * 跨父类查找名为 {@code key} 的字段（复刻 {@link #getAllField} 的层级遍历），找到则 setAccessible 一次后返回。
     *
     * @return key 字段；未找到返回 null
     */
    private static Accessor findAccessor(Class<?> clazz, String key) {
        String capitalized = Character.toUpperCase(key.charAt(0)) + key.substring(1);
        for (Class<?> c = clazz; c != null && c != Object.class; c = c.getSuperclass()) {
            try {
                Method m = c.getDeclaredMethod("get" + capitalized);
                setAccessible(m);
                return new Accessor(m, null);
            } catch (NoSuchMethodException ignored) {
            }
            try {
                Method m = c.getDeclaredMethod("is" + capitalized);
                setAccessible(m);
                return new Accessor(m, null);
            } catch (NoSuchMethodException ignored) {
            }
        }
        for (Class<?> c = clazz; c != null && c != Object.class; c = c.getSuperclass()) {
            try {
                Field f = c.getDeclaredField(key);
                setAccessible(f);
                return new Accessor(null, f);
            } catch (NoSuchFieldException ignored) {
            }
        }
        return null;
    }

    public static Class<?> getFieldParameterizedType(Field field) {
        if (field.getType().isArray()) {
            return field.getType().getComponentType();
        }
        Type genericType = field.getGenericType();
        if (genericType instanceof ParameterizedType pt && pt.getActualTypeArguments().length > 0) {
            return (Class<?>) pt.getActualTypeArguments()[0];
        }
        // 裸类型 / 类型变量 / 无类型参数：无法确定集合元素类型，回退为 Object（避免 ClassCastException / NPE）
        return Object.class;
    }

    public static Class<?> getWrapperClass(Class<?> clazz) {
        if (clazz == null) {
            return null;
        }
        if (clazz.isPrimitive()) {
            return PRIMITIVE_WRAPPER_MAP.get(clazz);
        }
        return clazz;
    }

}
