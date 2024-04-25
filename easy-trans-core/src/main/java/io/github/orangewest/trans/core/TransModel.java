package io.github.orangewest.trans.core;


import io.github.orangewest.trans.util.CollectionUtils;
import io.github.orangewest.trans.util.ReflectUtils;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

public class TransModel {

    /**
     * object value 提取标识
     */
    public final static String VAL_EXTRACT = "#val";

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

    private final boolean isValExtract;

    public TransModel(Object obj, TransFieldMeta field) {
        this.transFieldMeta = field;
        this.obj = obj;
        Field transField = field.getTransField();
        Class<?> type = transField.getType();
        this.isMultiple = (Iterable.class).isAssignableFrom(type) || type.isArray();
        this.transVal = ReflectUtils.getFieldValue(this.obj, transField);
        this.isValExtract = VAL_EXTRACT.equals(this.transFieldMeta.getKey());
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
                    if (this.isValExtract) {
                        for (Map<?, ?> objMap : objValMap.values()) {
                            objCollection.add(objMap.get(val));
                        }
                    } else {
                        Map<?, ?> objMap = objValMap.get(val);
                        if (objMap != null) {
                            objCollection.add(objMap.get(this.transFieldMeta.getKey()));
                        }
                    }
                });
            } else if (objValue instanceof Object[]) {
                Object[] objArray = (Object[]) objValue;
                for (int i = 0; i < multipleTransVal.size(); i++) {
                    if (this.isValExtract) {
                        for (Map<?, ?> objMap : objValMap.values()) {
                            objArray[i] = objMap.get(multipleTransVal.get(i));
                        }
                    } else {
                        Map<?, ?> objMap = objValMap.get(multipleTransVal.get(i));
                        if (objMap != null) {
                            objArray[i] = objMap.get(this.transFieldMeta.getKey());
                        }
                    }
                }
            }
        } else {
            if (this.isValExtract) {
                for (Map<?, ?> value : objValMap.values()) {
                    objValue = value.get(this.transVal);
                }
            } else {
                Map<?, ?> objMap = objValMap.get(this.transVal);
                if (objMap != null) {
                    objValue = objMap.get(this.transFieldMeta.getKey());
                }
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

    public boolean isValExtract() {
        return isValExtract;
    }

}
