package io.github.orangewest.trans;

import io.github.orangewest.trans.annotation.TransRepo;
import io.github.orangewest.trans.repository.TeacherTransRepository;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD})
@TransRepo(using = TeacherTransRepository.class)
public @interface TeacherTransRepo {

    String name() default "";

}
