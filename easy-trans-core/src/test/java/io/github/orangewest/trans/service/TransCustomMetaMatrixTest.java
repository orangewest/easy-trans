package io.github.orangewest.trans.service;

import io.github.orangewest.trans.annotation.Trans;
import io.github.orangewest.trans.annotation.TransRepo;
import io.github.orangewest.trans.repository.CityTransRepository;
import io.github.orangewest.trans.repository.TransRepositoryFactory;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * #04 测试矩阵 —— 自定义 {@code @Trans} 各形态外部行为。
 *
 * <ul>
 *   <li>独立自定义 {@code @Trans}（{@code using=} 现有仓库）：见 {@link TransCustomMetaTransTest}（#02）</li>
 *   <li>字典式自定义 {@code @Trans} + extra 属性：见 {@link TransDictCustomTransTest}（#03）</li>
 *   <li>普通 {@code @Trans(trans, using)} 回归：见 {@link TransServiceTest}（#01 之前即存在）</li>
 * </ul>
 *
 * 本类聚焦新增的「<b>多级嵌套链中使用自定义 {@code @Trans}</b>」场景：用自定义 {@code @CityIdTrans}
 * （元标注 {@code @Trans(trans="areaId", key="pid", using=CityTransRepository)}）把
 * {@code areaId -> CityEntity.pid} 桥接为下一级的 {@code cityId}，再经普通 {@code @Trans} 逐级翻译。
 * 证明自定义 {@code @Trans} 能无缝嵌入多级 {@code buildTransTree} 链路。
 */
class TransCustomMetaMatrixTest {

    /** 自定义 @Trans 元注解：从 areaId 取 CityEntity.pid 桥接为下一级 cityId。 */
    @Target(ElementType.FIELD)
    @Retention(RetentionPolicy.RUNTIME)
    @Trans(trans = "areaId", key = "pid", using = CityTransRepository.class)
    @interface CityIdTrans {
    }

    static class NestedCustomDto {
        @TransRepo(using = CityTransRepository.class)
        private Long areaId;

        @CityIdTrans
        @TransRepo(using = CityTransRepository.class)
        private Long cityId;

        @Trans(trans = "cityId", key = "name")
        private String cityName;

        @Trans(trans = "cityId", key = "pid")
        private Long provinceId;

        @Trans(trans = "provinceId", key = "name", using = CityTransRepository.class)
        private String provinceName;

        NestedCustomDto(Long areaId) {
            this.areaId = areaId;
        }
    }

    @BeforeAll
    static void setup() {
        TransRepositoryFactory.register(new CityTransRepository());
    }

    @Test
    void nestedChainWithCustomTrans() {
        TransService service = new TransService();
        NestedCustomDto dto = new NestedCustomDto(7L);

        boolean result = service.trans(dto);

        assertTrue(result);
        assertEquals(2L, dto.cityId);             // areaId=7 -> 长沙县.pid=2
        assertEquals("长沙市", dto.cityName);        // cityId=2 -> 长沙市.name
        assertEquals(1L, dto.provinceId);          // cityId=2 -> 长沙市.pid=1
        assertEquals("湖南省", dto.provinceName);    // provinceId=1 -> 湖南省.name
    }
}
