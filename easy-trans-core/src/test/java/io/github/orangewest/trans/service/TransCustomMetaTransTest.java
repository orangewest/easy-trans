package io.github.orangewest.trans.service;

import io.github.orangewest.trans.annotation.Trans;
import io.github.orangewest.trans.annotation.TransRepo;
import io.github.orangewest.trans.dto.TeacherDto;
import io.github.orangewest.trans.repository.TeacherTransRepository;
import io.github.orangewest.trans.repository.TransRepository;
import io.github.orangewest.trans.repository.TransRepositoryFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * 验证 #02：自定义「@Trans 元注解」能被 {@code parseTransField} 识别并正常翻译。
 * {@code @MyTrans} 本身被 {@code @Trans} 元标注，挂在目标字段上，框架把它当作 @Trans 处理。
 */
class TransCustomMetaTransTest {

    /** 自定义翻译注解：等价于 @Trans(trans = "teacherId", key = "name", using = TeacherTransRepository.class) */
    @Target(ElementType.FIELD)
    @Retention(RetentionPolicy.RUNTIME)
    @Trans(trans = "teacherId", key = "name", using = TeacherTransRepository.class)
    @interface MyTrans {
    }

    static class CustomMetaDto {
        @TransRepo(using = TeacherTransRepository.class)
        private Long teacherId;

        @MyTrans
        private String teacherName;

        CustomMetaDto(Long teacherId) {
            this.teacherId = teacherId;
        }

        public Long getTeacherId() {
            return teacherId;
        }

        public String getTeacherName() {
            return teacherName;
        }
    }

    @BeforeAll
    static void setup() {
        TransRepositoryFactory.register(new TeacherTransRepository());
    }

    @Test
    void customMetaTrans_isRecognizedAndTranslated() {
        TransService service = new TransService();
        CustomMetaDto dto = new CustomMetaDto(2L);
        boolean result = service.trans(dto);

        assertEquals(true, result);
        assertNotNull(dto.getTeacherName());
        assertEquals("老师2", dto.getTeacherName());
    }

}
