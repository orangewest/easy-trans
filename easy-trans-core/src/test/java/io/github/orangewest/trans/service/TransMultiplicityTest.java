package io.github.orangewest.trans.service;

import io.github.orangewest.trans.annotation.Trans;
import io.github.orangewest.trans.exception.TransException;
import io.github.orangewest.trans.repository.TransContext;
import io.github.orangewest.trans.repository.TransRepository;
import io.github.orangewest.trans.repository.TransRepositoryFactory;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * R5：多值判定的权威来源从「源仓库字段」改为「目标字段」(TransFieldMeta.isMultiple)。
 * 锁定两个发散场景：源单值→目标集合（此前漏填，现修复）、源集合→目标单值（语义不明确，启动期 fail-fast）。
 */
class TransMultiplicityTest {

    static class KVRepo implements TransRepository<Long, String> {
        @Override
        public Map<Long, String> getTransValueMap(List<Long> transValues, TransContext context) {
            Map<Long, String> m = new HashMap<>();
            for (Long k : transValues) {
                m.put(k, "v" + k);
            }
            return m;
        }
    }

    /** 源单值 (id) -> 目标集合 (names) */
    static class SingleSourceMultiTargetDto {
        private Long id;

        @Trans(trans = "id", using = KVRepo.class)
        private List<String> names;

        SingleSourceMultiTargetDto(Long id) {
            this.id = id;
        }

        public Long getId() {
            return id;
        }

        public List<String> getNames() {
            return names;
        }
    }

    /** 源集合 (ids) -> 目标单值 (name)：语义不明确，应启动期 fail-fast */
    static class MultiSourceSingleTargetDto {
        private List<Long> ids;

        @Trans(trans = "ids", using = KVRepo.class)
        private String name;

        MultiSourceSingleTargetDto(List<Long> ids) {
            this.ids = ids;
        }

        public List<Long> getIds() {
            return ids;
        }

        public String getName() {
            return name;
        }
    }

    @BeforeAll
    static void before() {
        TransRepositoryFactory.register(new KVRepo());
    }

    @Test
    void singleSourceToMultiTarget_isFilled() {
        TransService service = new TransService();
        SingleSourceMultiTargetDto dto = new SingleSourceMultiTargetDto(1L);
        service.trans(dto);
        Assertions.assertNotNull(dto.getNames(), "目标集合应被填充，而非漏填");
        Assertions.assertEquals(List.of("v1"), dto.getNames());
    }

    @Test
    void multiSourceToSingleTarget_failsFast() {
        TransService service = new TransService();
        Assertions.assertThrows(TransException.class,
                () -> service.trans(new MultiSourceSingleTargetDto(List.of(1L, 2L))));
    }
}
