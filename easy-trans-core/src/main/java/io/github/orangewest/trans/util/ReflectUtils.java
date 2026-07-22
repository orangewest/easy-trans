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

    public static final Map<Class<?>, Class<?>> WRAPPER_PRIMITIVE_MAP = new ConcurrentHashMap<>(8);

    public static final Map<Class<?>, Class<?>> PRIMITIVE_WRAPPER_MAP = new ConcurrentHashMap<>(8);

    /**
     * key 字段懒缓存的复合键：运行期类 + 要提取的 key 字段名。
     * 同一 {@code R} 类型可能被多个目标字段以不同 {@code key} 提取（如 {@code CityEntity} 同时被取 {@code name}/{@code pid}），
     * 故缓存键须同时含 key 名，否则会串味（见 ADR-0003）。
     */
    private record KeyField(Class<?> clazz, String key) {
    }

    /**
     * key 字段懒缓存：复合键 -> 该类的 key 字段（已 setAccessible）。
     * 值为 {@link Optional} 以兼容「该类无 key 字段」(null) 的缓存，避免 {@code computeIfAbsent} 禁用的 null value。
     */
    private static final Map<KeyField, Optional<Field>> KEY_FIELD_CACHE = new ConcurrentHashMap<>();

    static {
        WRAPPER_PRIMITIVE_MAP.put(Boolean.class, boolean.class);
        WRAPPER_PRIMITIVE_MAP.put(Byte.class, byte.class);
        WRAPPER_PRIMITIVE_MAP.put(Character.class, char.class);
        WRAPPER_PRIMITIVE_MAP.put(Double.class, double.class);
        WRAPPER_PRIMITIVE_MAP.put(Float.class, float.class);
        WRAPPER_PRIMITIVE_MAP.put(Integer.class, int.class);
        WRAPPER_PRIMITIVE_MAP.put(Long.class, long.class);
        WRAPPER_PRIMITIVE_MAP.put(Short.class, short.class);
        for (Map.Entry<Class<?>, Class<?>> entry : WRAPPER_PRIMITIVE_MAP.entrySet()) {
            PRIMITIVE_WRAPPER_MAP.put(entry.getValue(), entry.getKey());
        }
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
        } catch (IllegalAccessException ignored) {
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
        Field field = KEY_FIELD_CACHE.computeIfAbsent(new KeyField(value.getClass(), key), k -> Optional.ofNullable(findKeyField(k.clazz(), k.key())))
                .orElse(null);
        if (field == null) {
            return null;
        }
        try {
            return field.get(value);
        } catch (IllegalAccessException e) {
            return null;
        }
    }

    /**
     * 跨父类查找名为 {@code key} 的字段（复刻 {@link #getAllField} 的层级遍历），找到则 setAccessible 一次后返回。
     *
     * @return key 字段；未找到返回 null
     */
    private static Field findKeyField(Class<?> clazz, String key) {
        for (Class<?> c = clazz; c != null && c != Object.class; c = c.getSuperclass()) {
            try {
                Field f = c.getDeclaredField(key);
                setAccessible(f);
                return f;
            } catch (NoSuchFieldException ignored) {
                // 当前类无此字段，继续向上找父类
            }
        }
        return null;
    }

    public static boolean isPrimitiveWrapper(Class<?> clazz) {
        if (null == clazz) {
            return false;
        }
        return WRAPPER_PRIMITIVE_MAP.containsKey(clazz);
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
