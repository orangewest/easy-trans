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
        this.isMultiple = transFieldMeta.getTransRepoMeta().isMultiple();
        this.transVal = transField == null ? obj : ReflectUtils.getFieldValue(obj, transField);
    }

    public void fillValue(Map<Object, Object> transValueMap) {
        Object objValue;
        boolean isFillAll = transValueMap.values().stream().anyMatch(transValue -> transValue.getClass().isAssignableFrom(this.transFieldMeta.getFieldType()));
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
                    multipleTransVal.forEach(val -> {
                        Map<?, ?> objMap = objValMap.get(val);
                        if (objMap != null) {
                            objCollection.add(objMap.get(this.transFieldMeta.getKey()));
                        }
                    });
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
                    for (int i = 0; i < multipleTransVal.size(); i++) {
                        Map<?, ?> objMap = objValMap.get(multipleTransVal.get(i));
                        if (objMap != null) {
                            objArray[i] = objMap.get(this.transFieldMeta.getKey());
                        }
                    }
                }
            }
        } else {
            if (isFillAll) {
                objValue = transValueMap.get(this.transVal);
            } else {
                Map<?, ?> objValMap = ReflectUtils.beanToMap(transValueMap.get(this.transVal));
                objValue = objValMap.get(this.transFieldMeta.getKey());
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
