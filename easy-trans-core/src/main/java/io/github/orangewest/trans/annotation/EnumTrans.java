package io.github.orangewest.trans.annotation;

import io.github.orangewest.trans.repository.enumdict.EnumTransRepository;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 枚举即字典：把一个 enum 直接当作翻译源，免写自定义 {@code TransRepository}。
 * <p>
 * 等价于 {@code @Trans(using = EnumTransRepository.class)}，标注在<b>目标字段</b>上。
 * 枚举类默认从源字段类型推断（源字段本身为 enum 时无需指定）；当源字段是 code（如 Integer / String）时，
 * 用 {@link #enumClass()} 显式指定，并用 {@link #code()} 指明按枚举的哪个字段匹配源值。
 * 展示值从匹配到的枚举常量中按 {@link #key()} 取出（默认 {@code "label"}，须为 public 字段）。
 * <p>
 * 例：
 * <pre>{@code
 * private Sex sex;                       // 源字段（enum）
 * @EnumTrans(trans = "sex")             // 目标字段，key 默认取 label
 * private String sexName;
 *
 * private Integer sexCode;              // 源字段（code）
 * @EnumTrans(trans = "sexCode", enumClass = Sex.class, code = "code")
 * private String sexName;
 * }</pre>
 */
@Trans(using = EnumTransRepository.class)
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.ANNOTATION_TYPE})
public @interface EnumTrans {

    /**
     * @return 源字段名（持有原始 key 的字段）。必填。
     */
    String trans() default "";

    /**
     * @return 从匹配到的枚举常量中取出的字段名（如 {@code "label"}）。默认 {@code "label"}，须为 public 字段。
     */
    String key() default "label";

    /**
     * 枚举类。默认（Void.class）时由源字段类型推断（源字段须为 enum）；
     * 源字段是 code 时须显式指定。
     */
    Class<?> enumClass() default Void.class;

    /**
     * 用枚举的哪个字段匹配源值（如 "code"）。
     * 为空时按 name()（String 源）/ ordinal()（int 源）/ 恒等（源即枚举）匹配。
     */
    String code() default "";
}
