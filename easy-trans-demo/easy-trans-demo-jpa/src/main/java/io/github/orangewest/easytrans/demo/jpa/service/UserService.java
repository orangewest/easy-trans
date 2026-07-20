package io.github.orangewest.easytrans.demo.jpa.service;

import io.github.orangewest.easytrans.demo.jpa.dto.UserDto;
import io.github.orangewest.trans.spring.annotation.AutoTrans;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    /**
     * 返回未翻译的原始 DTO，由 {@code @AutoTrans} 切面在返回后自动翻译。
     */
    @AutoTrans
    public UserDto getUser() {
        UserDto user = new UserDto();
        user.setId(2L);
        user.setName("李四");
        user.setTeacherId(2L);
        return user;
    }
}
