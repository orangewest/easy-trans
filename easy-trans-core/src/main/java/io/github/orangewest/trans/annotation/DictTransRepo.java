package io.github.orangewest.trans.annotation;

import io.github.orangewest.trans.repository.dict.DictTransRepository;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 字典翻译仓库注解
 */
@TransRepo(using = DictTransRepository.class)
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface DictTransRepo {

    /**
     * @return 仓库名称（默认使用字段名）
     */
    String name() default "";

    /**
     * 字典组
     *
     * @return 字典分组
     */
    String group();

}
