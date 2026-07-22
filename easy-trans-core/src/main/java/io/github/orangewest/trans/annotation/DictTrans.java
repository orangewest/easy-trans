package io.github.orangewest.trans.annotation;

import io.github.orangewest.trans.repository.dict.DictTransRepository;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 字典翻译：等价于 {@code @Trans(using = DictTransRepository.class)}，标注在<b>目标字段</b>上，
 * 由 {@link #group()} 区分不同字典。源字段由 {@link #trans()} 指定（持有原始 code 的字段）。
 * <p>
 * 例：
 * <pre>{@code
 * private Integer sex;
 * @DictTrans(group = "sex", trans = "sex")
 * private String sexName;
 * }</pre>
 */
@Trans(using = DictTransRepository.class)
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.ANNOTATION_TYPE})
public @interface DictTrans {

    /**
     * @return 源字段名（持有原始 code 的字段）。必填。
     */
    String trans() default "";

    /**
     * @return 从字典值中取出的字段名；默认取目标字段名。
     */
    String key() default "";

    /**
     * 字典组
     *
     * @return 字典分组
     */
    String group();
}
