package io.github.orangewest.easytrans.demo.mybatis.annotation;

import io.github.orangewest.easytrans.demo.mybatis.entity.BaseEntity;
import io.github.orangewest.easytrans.demo.mybatis.repository.DbTransRepository;
import io.github.orangewest.trans.annotation.TransRepo;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 基于数据库的翻译仓库注解（与 {@code @DictTransRepo} 同一套路）：把<b>源字段</b>绑定到
 * {@link DbTransRepository}，并通过 {@code entity()} 指定要查的数据库实体类。
 *
 * <p>用法：源字段用本注解声明，目标字段用普通 {@code @Trans(trans = 源字段名)} 引用即可。
 * <pre>{@code
 * @DbTransRepo(entity = Teacher.class)
 * private Long teacherId;                 // 源字段：存放待翻译的 id
 *
 * @Trans(trans = "teacherId")             // 整体对象填充：目标字段类型与实体一致 → 填入整对象
 * private Teacher teacher;
 *
 * @Trans(trans = "teacherId", key = "name") // 属性提取：从实体中取 name 填入
 * private String teacherName;
 * }</pre>
 *
 * <p>框架在解析阶段把 {@code entity} 经
 * {@link io.github.orangewest.trans.repository.TransContext} 传给 {@link DbTransRepository}，
 * 运行期零反射；该自定义元注解也会被 {@code EasyTransRuntimeHints} 自动识别并注册 Native Image 反射元数据。
 */
@TransRepo(using = DbTransRepository.class)
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface DbTransRepo {

    /** 仓库名称（默认使用字段名） */
    String name() default "";

    /** 目标数据库实体类 */
    Class<? extends BaseEntity> entity() default BaseEntity.class;
}
