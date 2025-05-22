package io.github.orangewest.trans.core;

import io.github.orangewest.trans.repository.TransRepository;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.List;

public class TransFieldMeta {

    /**
     * 需要被翻译的属性名称
     */
    private final String trans;

    /**
     * 需要被翻译的属性
     */
    private final Field transField;

    /**
     * 需要翻译的属性
     */
    private final Field field;

    /**
     * 提取字段的key
     */
    private final String key;

    private final Annotation transAnno;

    /**
     * 翻译仓库
     */
    private final Class<? extends TransRepository<?, ?>> repository;

    /**
     * 子属性
     */
    private List<TransFieldMeta> children;

    public TransFieldMeta(Field field, Field transField, String key, Class<? extends TransRepository<?, ?>> repository, Annotation transAnno) {
        this.field = field;
        this.transField = transField;
        this.trans = transField.getName();
        this.key = key;
        this.repository = repository;
        this.transAnno = transAnno;
    }

    public String getTrans() {
        return trans;
    }

    public Field getTransField() {
        return transField;
    }

    public Field getField() {
        return field;
    }

    public String getKey() {
        return key;
    }

    public Class<? extends TransRepository<?, ?>> getRepository() {
        return repository;
    }

    public List<TransFieldMeta> getChildren() {
        return children;
    }

    public void setChildren(List<TransFieldMeta> children) {
        this.children = children;
    }

    public Annotation getTransAnno() {
        return transAnno;
    }

}
