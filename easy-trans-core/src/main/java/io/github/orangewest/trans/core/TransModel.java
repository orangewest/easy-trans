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

    /**
     * 是否是值提取
     */
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

    public void fillValue(Map<Object, Object> transValueMap) {
        Object objValue = null;
        boolean isFillAll = transValueMap.values().stream().anyMatch(transValue -> transValue.getClass().isAssignableFrom(this.transFieldMeta.getField().getType()));
        if (this.isMultiple) {
            List<Object> multipleTransVal = getMultipleTransVal();
            objValue = getObjValue(multipleTransVal);
            if (objValue instanceof Collection) {
                @SuppressWarnings("unchecked")
                Collection<Object> objCollection = (Collection<Object>) objValue;
                if (isFillAll) {
                    multipleTransVal.forEach(val -> objCollection.add(transValueMap.get(val)));
                } else {
                    Map<Object, ? extends Map<?, ?>> objValMap = transValueMap.entrySet().stream()
                            .collect(Collectors.toMap(Map.Entry::getKey, x -> ReflectUtils.beanToMap(x.getValue())));
                    if (this.isValExtract) {
                        multipleTransVal.forEach(val -> {
                            for (Map<?, ?> objMap : objValMap.values()) {
                                objCollection.add(objMap.get(val));
                            }
                        });
                    } else {
                        multipleTransVal.forEach(val -> {
                            Map<?, ?> objMap = objValMap.get(val);
                            if (objMap != null) {
                                objCollection.add(objMap.get(this.transFieldMeta.getKey()));
                            }
                        });
                    }
                }
            } else if (objValue instanceof Object[]) {
                Object[] objArray = (Object[]) objValue;
                if (isFillAll) {
                    for (int i = 0; i < multipleTransVal.size(); i++) {
                        objArray[i] = transValueMap.get(multipleTransVal.get(i));
                    }
                } else {
                    Map<Object, ? extends Map<?, ?>> objValMap = transValueMap.entrySet().stream()
                            .collect(Collectors.toMap(Map.Entry::getKey, x -> ReflectUtils.beanToMap(x.getValue())));
                    if (this.isValExtract) {
                        for (int i = 0; i < multipleTransVal.size(); i++) {
                            for (Map<?, ?> objMap : objValMap.values()) {
                                objArray[i] = objMap.get(multipleTransVal.get(i));
                            }
                        }
                    } else {
                        for (int i = 0; i < multipleTransVal.size(); i++) {
                            Map<?, ?> objMap = objValMap.get(multipleTransVal.get(i));
                            if (objMap != null) {
                                objArray[i] = objMap.get(this.transFieldMeta.getKey());
                            }
                        }
                    }
                }
            }
        } else {
            if (isFillAll) {
                objValue = transValueMap.get(this.transVal);
            } else {
                if (this.isValExtract) {
                    for (Object value : transValueMap.values()) {
                        Map<?, ?> objValMap = ReflectUtils.beanToMap(value);
                        objValue = objValMap.get(this.transVal);
                    }
                } else {
                    Map<?, ?> objValMap = ReflectUtils.beanToMap(transValueMap.get(this.transVal));
                    objValue = objValMap.get(this.transFieldMeta.getKey());
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
