package io.github.orangewest.trans.annotation;

import io.github.orangewest.trans.core.TransModel;
import io.github.orangewest.trans.repository.dict.DictTransRepository;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Trans(using = DictTransRepository.class, key = TransModel.VAL_EXTRACT)
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface DictTrans {


    /**
     * @return 需要翻译的字段
     */
    String trans();

    /**
     * 字典组
     *
     * @return 字典分组
     */
    String group();

}
