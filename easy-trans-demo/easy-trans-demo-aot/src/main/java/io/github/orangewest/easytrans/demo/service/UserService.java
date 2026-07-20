package io.github.orangewest.easytrans.demo.service;

import io.github.orangewest.easytrans.demo.dto.UserDto;
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
        user.setId(2);
        user.setName("李四");
        user.setSex(2);
        user.setDictSex("1");
        user.setTeacherId(1);
        return user;
    }
}
