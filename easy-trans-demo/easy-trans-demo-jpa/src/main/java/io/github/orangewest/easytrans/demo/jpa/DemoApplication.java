package io.github.orangewest.easytrans.demo.jpa;

import io.github.orangewest.easytrans.demo.jpa.dto.UserDto;
import io.github.orangewest.easytrans.demo.jpa.service.UserService;
import io.github.orangewest.trans.service.TransService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

/**
 * Spring Data JPA 集成示例（基于 BaseEntity + DbTransRepo 的通用数据库翻译）：
 * <ul>
 *   <li>Entity / 数据源由 spring-boot-starter-data-jpa 自动装配（H2 内存库）</li>
 *   <li>{@code DbTransRepository}（TransRepository）经 {@code JpaTransDriver} 查库完成翻译</li>
 * </ul>
 */
@SpringBootApplication
public class DemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }

    @Bean
    CommandLineRunner runner(TransService transService, UserService userService) {
        return args -> {
            try {
                // 1) 直接调用 TransService.trans（数据来自 JPA 查询）
                UserDto u = new UserDto();
                u.setId(1L);
                u.setName("张三");
                u.setTeacherId(1L);
                transService.trans(u);
                System.out.println(">>> [TransService] " + u);
                check("teacherName", u.getTeacherName(), "老师1");
                check("teacher.name", u.getTeacher() == null ? null : u.getTeacher().getName(), "老师1");
                check("teacher.sex", u.getTeacher() == null ? null : String.valueOf(u.getTeacher().getSex()), "2");
                check("teacher.id", u.getTeacher() == null ? null : String.valueOf(u.getTeacher().getId()), "1");

                // 2) 经 @AutoTrans 切面自动翻译
                UserDto u2 = userService.getUser();
                System.out.println(">>> [@AutoTrans]  " + u2);
                check("teacherName", u2.getTeacherName(), "老师2");
                check("teacher.sex", u2.getTeacher() == null ? null : String.valueOf(u2.getTeacher().getSex()), "1");

                System.out.println(">>> ALL CHECKS PASSED");
            } catch (Throwable t) {
                System.err.println(">>> CHECK FAILED: " + t.getMessage());
            }
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
