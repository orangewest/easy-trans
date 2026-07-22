package io.github.orangewest.trans.service;

import io.github.orangewest.trans.annotation.Trans;
import io.github.orangewest.trans.exception.TransException;
import io.github.orangewest.trans.repository.TransContext;
import io.github.orangewest.trans.repository.TransRepository;
import io.github.orangewest.trans.repository.TransRepositoryFactory;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

/**
 * R8：多仓库并行翻译时，任一分组失败不再只抛首个 cause，而是聚合所有分组的失败
 * （首个作 cause，其余挂为 suppressed），便于一次排查多个仓库的归因。
 */
class TransMultiRepoErrorAggregationTest {

    static class BoomRepoA implements TransRepository<Long, String> {
        @Override
        public Map<Long, String> getTransValueMap(List<Long> transValues, TransContext context) {
            throw new RuntimeException("A boom");
        }
    }

    static class BoomRepoB implements TransRepository<Long, String> {
        @Override
        public Map<Long, String> getTransValueMap(List<Long> transValues, TransContext context) {
            throw new RuntimeException("B boom");
        }
    }

    static class TwoRepoDto {
        @Trans(trans = "idA", using = BoomRepoA.class)
        private String a;

        @Trans(trans = "idB", using = BoomRepoB.class)
        private String b;

        private final Long idA = 1L;
        private final Long idB = 2L;

        public Long getIdA() {
            return idA;
        }

        public Long getIdB() {
            return idB;
        }
    }

    @BeforeAll
    static void before() {
        TransRepositoryFactory.register(new BoomRepoA());
        TransRepositoryFactory.register(new BoomRepoB());
    }

    @Test
    void aggregatesAllRepoFailures() {
        TransService service = new TransService();
        TransException ex = Assertions.assertThrows(TransException.class,
                () -> service.trans(new TwoRepoDto()));

        StringBuilder combined = new StringBuilder(ex.getMessage());
        if (ex.getCause() != null) {
            combined.append(ex.getCause().getMessage());
        }
        for (Throwable suppressed : ex.getSuppressed()) {
            combined.append(suppressed.getMessage());
        }
        String all = combined.toString();
        Assertions.assertTrue(all.contains("A boom"), "聚合异常应含仓库 A 的失败归因");
        Assertions.assertTrue(all.contains("B boom"), "聚合异常应含仓库 B 的失败归因");
    }
}
