package io.github.orangewest.easytrans.demo.service;

import io.github.orangewest.easytrans.demo.dto.UserDto;
import io.github.orangewest.trans.spring.annotation.AutoTrans;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class ReactiveUserService {

    /**
     * 返回未翻译的 Mono，由 {@code @AutoTrans} 切面在返回值上自动翻译（Mono 经 ReactorTransResolver 处理）。
     */
    @AutoTrans
    public Mono<UserDto> getUser() {
        UserDto user = new UserDto();
        user.setId(2);
        user.setName("李四");
        user.setSex(2);
        user.setDictSex("1");
        user.setTeacherId(1);
        return Mono.just(user);
    }
}
