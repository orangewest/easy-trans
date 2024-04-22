package io.github.orangewest.trans.annotation;

import io.github.orangewest.trans.repository.dict.DictTransRepository;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Trans(using = DictTransRepository.class)
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface DictTrans {

    String trans();

    String key();

}
