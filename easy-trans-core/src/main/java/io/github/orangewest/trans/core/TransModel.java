package io.github.orangewest.trans.core;


import io.github.orangewest.trans.util.CollectionUtils;
import io.github.orangewest.trans.util.ReflectUtils;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

public class TransModel {

    /**
     * 需要被翻译的属性
     */
    private final TransFieldMeta transFieldMeta;

    /**
     * 需要被翻译的属性值
     */
    private final Object transVal;

    /**
     * 当前对象
     */
    private final Object obj;

    /**
     * 是否是多值
     */
    private final boolean isMultiple;


    public TransModel(Object obj, TransFieldMeta transFieldMeta) {
        this.transFieldMeta = transFieldMeta;
        this.obj = obj;
        Field transField = transFieldMeta.getTransRepoMeta().getRepoField();
        this.isMultiple = transFieldMeta.isMultiple();
        this.transVal = transField == null ? obj : ReflectUtils.getFieldValue(obj, transField);
    }

    public void fillValue(Map<Object, Object> transValueMap) {
        String key = transFieldMeta.getKey();
        boolean isFillAll = transValueMap.values().stream()
                .anyMatch(v -> v.getClass().isAssignableFrom(transFieldMeta.getFieldType()));
        Object filled = isMultiple
                ? fillMulti(transValueMap, key, isFillAll)
                : fillSingle(transValueMap, key, isFillAll);
        if (filled != null) {
            ReflectUtils.setFieldValue(obj, transFieldMeta.getField(), filled);
        }
    }

    private Object fillSingle(Map<Object, Object> map, String key, boolean isFillAll) {
        Object source = map.get(transVal);
        return isFillAll ? source : ReflectUtils.readValueByKey(source, key);
    }

    private Object fillMulti(Map<Object, Object> map, String key, boolean isFillAll) {
        List<Object> sources = getMultipleTransVal();
        Object container = getOrCreateContainer(sources);
        Map<Object, Object> resolved = isFillAll ? map : toKeyMap(map, key);
        for (Object src : sources) {
            Object val = resolved.get(src);
            if (val != null) {
                addToContainer(container, val);
            }
        }
        return container;
    }

    private static Map<Object, Object> toKeyMap(Map<Object, Object> map, String key) {
        return map.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> ReflectUtils.readValueByKey(e.getValue(), key)));
    }

    private static void addToContainer(Object container, Object value) {
        if (container instanceof Collection) {
            @SuppressWarnings("unchecked")
            Collection<Object> c = (Collection<Object>) container;
            c.add(value);
        } else if (container instanceof Object[] arr) {
            for (int i = 0; i < arr.length; i++) {
                if (arr[i] == null) {
                    arr[i] = value;
                    break;
                }
            }
        }
    }

    private Object getOrCreateContainer(List<Object> sources) {
        Object container = ReflectUtils.getFieldValue(obj, transFieldMeta.getField());
        if (container != null) {
            return container;
        }
        Class<?> type = transFieldMeta.getField().getType();
        if ((List.class).isAssignableFrom(type)) {
            return new ArrayList<>();
        }
        if ((Set.class).isAssignableFrom(type)) {
            return new HashSet<>();
        }
        if (type.isArray()) {
            return Array.newInstance(type.getComponentType(), sources.size());
        }
        return new ArrayList<>();
    }

    public TransFieldMeta getTransField() {
        return transFieldMeta;
    }

    public Object getTransVal() {
        return transVal;
    }

    public Object getObj() {
        return obj;
    }

    public boolean isMultiple() {
        return isMultiple;
    }

    public List<Object> getMultipleTransVal() {
        return CollectionUtils.objToList(this.transVal);
    }

    public boolean needTrans() {
        return transVal != null;
    }


}
