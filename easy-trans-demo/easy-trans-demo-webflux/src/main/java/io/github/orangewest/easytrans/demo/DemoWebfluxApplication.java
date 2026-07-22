package io.github.orangewest.easytrans.demo;

import io.github.orangewest.easytrans.demo.dto.UserDto;
import io.github.orangewest.easytrans.demo.service.ReactiveUserService;
import io.github.orangewest.trans.service.TransService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@SpringBootApplication
public class DemoWebfluxApplication {

    public static void main(String[] args) {
        SpringApplication.run(DemoWebfluxApplication.class, args);
    }

    @Bean
    CommandLineRunner runner(TransService transService, ReactiveUserService reactiveUserService) {
        return args -> {
            // 1) 普通同步翻译（确认基础能力在 native 下可用）
            UserDto u = new UserDto();
            u.setId(1);
            u.setName("张三");
            u.setSex(1);
            u.setDictSex("2");
            u.setTeacherId(2);
            transService.trans(u);
            check("sexName", u.getSexName(), "男");
            check("teacherName", u.getTeacherName(), "老师2");

            // 2) Reactor：Mono 翻译（验证 ReactorTransResolver 在 native 下可用）
            UserDto ru = new UserDto();
            ru.setId(1);
            ru.setName("张三");
            ru.setSex(1);
            ru.setDictSex("2");
            ru.setTeacherId(2);
            UserDto monoResult = ((Mono<UserDto>) transService.trans(Mono.just(ru))).block();
            check("reactor:sexName", monoResult.getSexName(), "男");
            check("reactor:teacherName", monoResult.getTeacherName(), "老师2");

            // 3) Reactor：Flux 翻译
            UserDto f1 = new UserDto();
            f1.setId(2);
            f1.setName("李四");
            f1.setSex(2);
            f1.setDictSex("1");
            f1.setTeacherId(1);
            UserDto f2 = new UserDto();
            f2.setId(1);
            f2.setName("张三");
            f2.setSex(1);
            f2.setDictSex("2");
            f2.setTeacherId(2);
            List<UserDto> fluxResult = ((Flux<UserDto>) transService.trans(Flux.just(f1, f2))).collectList().block();
            check("reactor:flux[0].sexName", fluxResult.get(0).getSexName(), "女");
            check("reactor:flux[1].teacherName", fluxResult.get(1).getTeacherName(), "老师2");

            // 4) @AutoTrans 切面 + Mono 返回值（ReactorTransResolver 与 AutoTransAspect 协作）
            UserDto autoMono = reactiveUserService.getUser().block();
            check("autotrans:reactor:sexName", autoMono.getSexName(), "女");
            check("autotrans:reactor:teacherName", autoMono.getTeacherName(), "老师1");

            System.out.println(">>> ALL REACTOR CHECKS PASSED");
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
