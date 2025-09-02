package io.github.orangewest.trans.core;

import io.github.orangewest.trans.util.ReflectUtils;

import java.lang.reflect.Field;
import java.util.List;

public class TransFieldMeta {

    /**
     * 翻译仓库信息
     */
    private final TransRepoMeta transRepoMeta;

    /**
     * 需要翻译的属性
     */
    private final Field field;

    /**
     * 属性类型
     */
    private final Class<?> fieldType;

    /**
     * 提取字段的key
     */
    private final String key;

    /**
     * 子属性
     */
    private List<TransFieldMeta> children;

    public TransFieldMeta(Field field, String key, TransRepoMeta transRepoMeta) {
        this.field = field;
        this.key = key;
        this.transRepoMeta = transRepoMeta;
        this.fieldType = transRepoMeta.isMultiple() ?
                ReflectUtils.getFieldParameterizedType(field) : ReflectUtils.getWrapperClass(field.getType());
    }

    public Field getField() {
        return field;
    }

    public String getKey() {
        return key;
    }

    public List<TransFieldMeta> getChildren() {
        return children;
    }

    public void setChildren(List<TransFieldMeta> children) {
        this.children = children;
    }

    public TransRepoMeta getTransRepoMeta() {
        return transRepoMeta;
    }

    public Class<?> getFieldType() {
        return fieldType;
    }


}
