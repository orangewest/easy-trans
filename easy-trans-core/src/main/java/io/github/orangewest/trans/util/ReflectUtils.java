package io.github.orangewest.trans.util;


import java.lang.annotation.Annotation;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Method;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ReflectUtils {

    public static final Map<Class<?>, Class<?>> WRAPPER_PRIMITIVE_MAP = new ConcurrentHashMap<>(8);

    public static final Map<Class<?>, Class<?>> PRIMITIVE_WRAPPER_MAP = new ConcurrentHashMap<>(8);

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

    /**
     * @param clazz class对象
     * @return 获取一个class的所有的字段
     */
    public static List<Field> getAllField(Class<?> clazz) {
        Field[] fields;
        List<Field> result = new ArrayList<>();
        for (; clazz != Object.class; clazz = clazz.getSuperclass()) {
            try {
                fields = clazz.getDeclaredFields();
                result.addAll(Arrays.asList(fields));
            } catch (Exception ignored) {
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
     * <p>只读取注解<b>自身声明的属性</b>（如自定义元注解 {@code @DictTransRepo} 的 {@code group()}），
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

    public static Object getFieldValue(Object obj, Field field) {
        if (null == field) {
            return null;
        } else {
            if (obj instanceof Class) {
                obj = null;
            }

            setAccessible(field);

            try {
                return field.get(obj);
            } catch (IllegalAccessException e) {
                return null;
            }
        }
    }

    public static <T extends AccessibleObject> void setAccessible(T accessibleObject) throws SecurityException {
        if (null != accessibleObject && !accessibleObject.isAccessible()) {
            accessibleObject.setAccessible(true);
        }
    }

    public static void setFieldValue(Object obj, Field field, Object fieldValue) {
        setAccessible(field);
        try {
            field.set(obj, fieldValue);
        } catch (IllegalAccessException ignored) {
        }
    }

    /**
     * @param bean 对象
     * @return 对象转Map
     */
    public static Map<?, ?> beanToMap(Object bean) {
        if (bean == null) {
            return Collections.emptyMap();
        }
        if (bean instanceof Map) {
            return (Map<?, ?>) bean;
        }
        List<Field> fields = getAllField(bean.getClass());
        Map<String, Object> map = new HashMap<>(fields.size());
        for (Field field : fields) {
            map.put(field.getName(), getFieldValue(bean, field));
        }
        return map;
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
        return (Class<?>) ((ParameterizedType) field.getGenericType()).getActualTypeArguments()[0];
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
