package io.github.orangewest.easytrans.demo.jpa;

import io.github.orangewest.easytrans.demo.jpa.dto.UserDto;
import io.github.orangewest.easytrans.demo.jpa.service.UserService;
import io.github.orangewest.trans.service.TransService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * JPA 集成端到端测试：验证基于 BaseEntity + DbTransRepo 的通用数据库翻译。
 */
@SpringBootTest
class DemoApplicationTests {

    @Autowired
    private TransService transService;

    @Autowired
    private UserService userService;

    @Test
    void transViaJpa() {
        UserDto u = new UserDto();
        u.setId(1L);
        u.setName("张三");
        u.setTeacherId(1L);

        transService.trans(u);

        assertNotNull(u.getTeacher(), "teacher 应被整体填充");
        assertEquals("老师1", u.getTeacherName());
        assertEquals("老师1", u.getTeacher().getName());
        assertEquals(Long.valueOf(1), u.getTeacher().getId());
        assertEquals(Integer.valueOf(2), u.getTeacher().getSex());
    }

    @Test
    void transViaAutoTransAspect() {
        UserDto u = userService.getUser();
        assertNotNull(u.getTeacher());
        assertEquals("老师2", u.getTeacherName());
        assertEquals(Integer.valueOf(1), u.getTeacher().getSex());
    }
}
