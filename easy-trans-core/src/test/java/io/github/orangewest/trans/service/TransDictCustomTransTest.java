package io.github.orangewest.trans.service;

import io.github.orangewest.trans.annotation.Trans;
import io.github.orangewest.trans.repository.TransRepository;
import io.github.orangewest.trans.repository.TransRepositoryFactory;
import io.github.orangewest.trans.repository.dict.DictLoader;
import io.github.orangewest.trans.repository.dict.DictTransRepository;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * 验证 #03：字典式自定义「@Trans 元注解」端到端生效。
 * {@code @DictTrans(group="sexDict")} 本身被 {@code @Trans(trans="dictSex", using=DictTransRepository.class)} 元标注，
 * 框架把 {@code group} 作为 extra 属性在解析期抽进 {@link io.github.orangewest.trans.repository.TransContext}，
 * {@link DictTransRepository} 经 {@code context.get("group", String.class)} 读取并按字典翻译。
 */
class TransDictCustomTransTest {

    /** 字典式自定义翻译注解：等价于 @Trans(trans = "dictSex", using = DictTransRepository.class) + 额外 group 属性 */
    @Target(ElementType.FIELD)
    @Retention(RetentionPolicy.RUNTIME)
    @Trans(trans = "dictSex", using = DictTransRepository.class)
    @interface DictTrans {
        String group();
    }

    static class DictCustomDto {
        private String dictSex;

        @DictTrans(group = "sexDict")
        private String sexName;

        DictCustomDto(String dictSex) {
            this.dictSex = dictSex;
        }

        public String getSexName() {
            return sexName;
        }
    }

    @BeforeAll
    static void setup() {
        TransRepositoryFactory.register(new DictTransRepository(new DictLoader() {
            @Override
            public Map<String, String> loadDict(String dictGroup) {
                Map<String, String> sexDict = new HashMap<>();
                sexDict.put("1", "男");
                sexDict.put("2", "女");
                return sexDict;
            }
        }));
    }

    @Test
    void dictCustomTrans_readsGroupFromContext() {
        TransService service = new TransService();
        DictCustomDto dto = new DictCustomDto("1");

        boolean result = service.trans(dto);

        assertEquals(true, result);
        assertNotNull(dto.getSexName());
        assertEquals("男", dto.getSexName());
    }

}
