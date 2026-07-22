package io.github.orangewest.easytrans.demo;

import io.github.orangewest.easytrans.demo.dto.UserDto;
import io.github.orangewest.easytrans.demo.service.UserService;
import io.github.orangewest.trans.service.TransService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class DemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }

    @Bean
    CommandLineRunner runner(TransService transService, UserService userService) {
        return args -> {
            // 1) 直接调用 TransService.trans（覆盖 @TransRepo/@Trans、@DictTrans、对象填充）
            UserDto u = new UserDto();
            u.setId(1);
            u.setName("张三");
            u.setSex(1);
            u.setDictSex("2");
            u.setTeacherId(2);
            transService.trans(u);
            System.out.println(">>> [TransService] " + u);
            check("sexName", u.getSexName(), "男");
            check("dictSexName", u.getDictSexName(), "女");
            check("teacher", u.getTeacher() == null ? null : u.getTeacher().getName(), "老师2");
            check("teacherName", u.getTeacherName(), "老师2");

            // 2) 经 @AutoTrans 切面自动翻译
            UserDto u2 = userService.getUser();
            System.out.println(">>> [@AutoTrans]  " + u2);
            check("sexName", u2.getSexName(), "女");
            check("dictSexName", u2.getDictSexName(), "男");
            check("teacher", u2.getTeacher() == null ? null : u2.getTeacher().getName(), "老师1");
            check("teacherName", u2.getTeacherName(), "老师1");

            System.out.println(">>> ALL CHECKS PASSED");
        };
    }

    private static void check(String field, String actual, String expected) {
        if (!expected.equals(actual)) {
            throw new IllegalStateException(
                "CHECK FAILED: " + field + " expected=[" + expected + "] actual=[" + actual + "]");
        }
        System.out.println("    [OK] " + field + " = " + actual);
    }
}
