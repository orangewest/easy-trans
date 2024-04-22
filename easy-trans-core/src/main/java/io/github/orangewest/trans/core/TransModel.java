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

    public TransModel(Object obj, TransFieldMeta field) {
        this.transFieldMeta = field;
        this.obj = obj;
        Field transField = field.getTransField();
        Class<?> type = transField.getType();
        this.isMultiple = (Iterable.class).isAssignableFrom(type) || type.isArray();
        this.transVal = ReflectUtils.getFieldValue(this.obj, transField);
    }

    public void setValue(Map<Object, Object> transValMap) {
        Map<Object, ? extends Map<?, ?>> objValMap = transValMap.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, entry -> ReflectUtils.beanToMap(entry.getValue())));
        Object objValue = null;
        if (this.isMultiple) {
            List<Object> multipleTransVal = getMultipleTransVal();
            objValue = getObjValue(multipleTransVal);
            if (objValue instanceof Collection) {
                @SuppressWarnings("unchecked")
                Collection<Object> objCollection = (Collection<Object>) objValue;
                multipleTransVal.forEach(val -> {
                    Map<?, ?> objMap = objValMap.get(val);
                    if (objMap != null) {
                        objCollection.add(objMap.get(this.transFieldMeta.getKey()));
                    }
                });
            } else if (objValue instanceof Object[]) {
                Object[] objArray = (Object[]) objValue;
                for (int i = 0; i < multipleTransVal.size(); i++) {
                    Map<?, ?> objMap = objValMap.get(multipleTransVal.get(i));
                    if (objMap != null) {
                        objArray[i] = objMap.get(this.transFieldMeta.getKey());
                    }
                }
            }
        } else {
            Map<?, ?> objMap = objValMap.get(this.transVal);
            if (objMap != null) {
                objValue = objMap.get(this.transFieldMeta.getKey());
            }
        }
        if (objValue != null) {
            ReflectUtils.setFieldValue(this.obj, this.transFieldMeta.getField(), objValue);
        }
    }

    public void setKeyValue(Map<Object, Object> transValMap) {
        Map<Object, ? extends Map<?, ?>> objValMap = transValMap.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, entry -> ReflectUtils.beanToMap(entry.getValue())));
        Object objValue = null;
        if (this.isMultiple) {
            List<Object> multipleTransVal = getMultipleTransVal();
            objValue = getObjValue(multipleTransVal);
            if (objValue instanceof Collection) {
                @SuppressWarnings("unchecked")
                Collection<Object> objCollection = (Collection<Object>) objValue;
                multipleTransVal.forEach(val -> {
                    Map<?, ?> objMap = objValMap.get(this.transFieldMeta.getKey());
                    if (objMap != null) {
                        objCollection.add(objMap.get(val));
                    }
                });
            } else if (objValue instanceof Object[]) {
                Object[] objArray = (Object[]) objValue;
                for (int i = 0; i < multipleTransVal.size(); i++) {
                    Map<?, ?> objMap = objValMap.get(this.transFieldMeta.getKey());
                    if (objMap != null) {
                        objArray[i] = objMap.get(multipleTransVal.get(i));
                    }
                }
            }
            ReflectUtils.setFieldValue(this.obj, this.transFieldMeta.getField(), objValue);
        } else {
            Map<?, ?> objMap = objValMap.get(this.transFieldMeta.getKey());
            if (objMap != null) {
                objValue = objMap.get(this.transVal);
            }
        }
        if (objValue != null) {
            ReflectUtils.setFieldValue(this.obj, this.transFieldMeta.getField(), objValue);
        }
    }

    private Object getObjValue(List<Object> multipleTransVal) {
        Object objValue = ReflectUtils.getFieldValue(this.obj, this.transFieldMeta.getField());
        if (objValue == null) {
            Class<?> type = this.transFieldMeta.getField().getType();
            if ((List.class).isAssignableFrom(type)) {
                objValue = new ArrayList<>();
            } else if ((Set.class).isAssignableFrom(type)) {
                objValue = new HashSet<>();
            } else if (type.isArray()) {
                objValue = Array.newInstance(type.getComponentType(), multipleTransVal.size());
            }
        }
        return objValue;
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
