package io.github.orangewest.trans.util;


import java.lang.annotation.Annotation;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.util.*;

public class ReflectUtils {

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
}
