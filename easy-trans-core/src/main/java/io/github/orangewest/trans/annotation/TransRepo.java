package io.github.orangewest.trans.annotation;

import io.github.orangewest.trans.repository.TransRepository;

import java.lang.annotation.*;

/**
 * 翻译仓库注解
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.ANNOTATION_TYPE, ElementType.TYPE})
@Repeatable(TransRepos.class)
public @interface TransRepo {

    /**
     * @return 仓库名称（默认使用字段名）
     */
    String name() default "";

    /**
     * @return 翻译数据获取仓库
     */
    Class<? extends TransRepository<?, ?>> using();
}
