package io.github.orangewest.easytrans.demo;

import io.github.orangewest.easytrans.demo.dto.UserDto;
import io.github.orangewest.easytrans.demo.service.UserService;
import io.github.orangewest.trans.service.TransService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class DemoActuatorApplication {

    public static void main(String[] args) {
        SpringApplication.run(DemoActuatorApplication.class, args);
    }

    /**
     * 启动期做几次翻译，使 {@code easytrans.translate} / {@code easytrans.repository} / {@code easytrans.field}
     * 指标产生数据；随后内嵌 web 服务器常驻，可通过
     * {@code GET /actuator/metrics/easytrans.translate} 与 {@code GET /actuator/prometheus} 观测。
     */
    @Bean
    CommandLineRunner warmup(TransService transService, UserService userService) {
        return args -> {
            for (int i = 0; i < 3; i++) {
                UserDto u = new UserDto();
                u.setId(1);
                u.setName("张三");
                u.setSex(1);
                u.setDictSex("2");
                u.setTeacherId(2);
                transService.trans(u);
                userService.getUser();
            }
            System.out.println(">>> demo-actuator ready: try GET /actuator/metrics/easytrans.translate");
        };
    }
}
