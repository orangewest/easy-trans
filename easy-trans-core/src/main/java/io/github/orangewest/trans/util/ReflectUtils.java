package io.github.orangewest.trans.util;


import java.lang.annotation.Annotation;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ReflectUtils {

    public static final Map<Class<?>, Class<?>> WRAPPER_PRIMITIVE_MAP = new ConcurrentHashMap<>(8);

    static {
        WRAPPER_PRIMITIVE_MAP.put(Boolean.class, boolean.class);
        WRAPPER_PRIMITIVE_MAP.put(Byte.class, byte.class);
        WRAPPER_PRIMITIVE_MAP.put(Character.class, char.class);
        WRAPPER_PRIMITIVE_MAP.put(Double.class, double.class);
        WRAPPER_PRIMITIVE_MAP.put(Float.class, float.class);
        WRAPPER_PRIMITIVE_MAP.put(Integer.class, int.class);
        WRAPPER_PRIMITIVE_MAP.put(Long.class, long.class);
        WRAPPER_PRIMITIVE_MAP.put(Short.class, short.class);
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

}
