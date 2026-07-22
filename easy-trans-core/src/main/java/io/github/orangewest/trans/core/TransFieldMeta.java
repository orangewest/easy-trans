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

    /**
     * 多值判定：以*目标字段*（field）自身的集合 / 数组性为准（R5）。
     * README 承诺「源或目标为集合/数组」均支持，故不依赖源仓库字段的多值性，
     * 避免「源单值 → 目标集合」被误判为单值而漏填。
     */
    private final boolean isMultiple;

    public TransFieldMeta(Field field, String key, TransRepoMeta transRepoMeta) {
        this.field = field;
        this.key = key;
        this.transRepoMeta = transRepoMeta;
        this.isMultiple = (Iterable.class).isAssignableFrom(field.getType()) || field.getType().isArray();
        this.fieldType = isMultiple ?
                ReflectUtils.getFieldParameterizedType(field) : ReflectUtils.getWrapperClass(field.getType());
        // 解析期一次性 setAccessible（ADR-0003）：运行期 getFieldValue/setFieldValue 不再重复检查
        ReflectUtils.setAccessible(field);
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

    public boolean isMultiple() {
        return isMultiple;
    }


}
