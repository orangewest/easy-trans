package io.github.orangewest.trans.annotation;

import io.github.orangewest.trans.repository.TransRepository;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 翻译注解
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.ANNOTATION_TYPE})
public @interface Trans {

    /**
     * @return 需要翻译的字段
     */
    String trans() default "";

    /**
     * @return 提取的字段
     */
    String key() default "";

    /**
     * @return 翻译数据获取仓库
     */
    Class<? extends TransRepository> using();

}
