package io.github.orangewest.trans.core;

import io.github.orangewest.trans.repository.TransRepository;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;

public class TransRepoMeta {

    /**
     * 仓库名称
     */
    private final String repoName;

    /**
     * 仓库对应的属性
     */
    private final Field repoField;

    /**
     * 是否是多值
     */
    private final boolean isMultiple;

    /**
     * 属性指定的翻译注解
     */
    private final Annotation repoAnno;

    /**
     * 属性使用的翻译仓库
     */
    private final Class<? extends TransRepository<?, ?>> repository;


    public TransRepoMeta(String repoName, Field repoField, Annotation repoAnno, Class<? extends TransRepository<?, ?>> repository) {
        this.repoName = repoField != null ? repoField.getName() : repoName;
        this.repoField = repoField;
        this.repoAnno = repoAnno;
        this.repository = repository;
        this.isMultiple = repoField != null &&
                ((Iterable.class).isAssignableFrom(repoField.getType()) || repoField.getType().isArray());
    }

    public String getRepoName() {
        return repoName;
    }

    public Field getRepoField() {
        return repoField;
    }

    public Annotation getRepoAnno() {
        return repoAnno;
    }

    public Class<? extends TransRepository<?, ?>> getRepository() {
        return repository;
    }

    public boolean isMultiple() {
        return isMultiple;
    }

}
