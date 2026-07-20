package io.github.orangewest.trans.service;

import io.github.orangewest.trans.annotation.Trans;
import io.github.orangewest.trans.exception.TransException;
import io.github.orangewest.trans.repository.TransContext;
import io.github.orangewest.trans.repository.TransRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 验证 #1：原本静默失败的场景（未初始化、仓库未注册）现在直接抛出 TransException。
 * 使用未被注册到 TransRepositoryFactory 的仓库，避免影响共享的 @BeforeAll 注册。
 */
class TransServiceFailureTest {

    /** 一个从未注册的翻译仓库 */
    static class UnregisteredRepo implements TransRepository<Long, String> {
        @Override
        public Map<Long, String> getTransValueMap(List<Long> transValues, TransContext context) {
            return new HashMap<Long, String>();
        }
    }

    /** 引用了未注册仓库的 DTO */
    static class UnregisteredRepoDto {
        private Long id;
        @Trans(trans = "id", using = UnregisteredRepo.class)
        private String name;

        UnregisteredRepoDto(Long id) {
            this.id = id;
        }

        public Long getId() {
            return id;
        }

        public String getName() {
            return name;
        }
    }

    @Test
    void missingRepo_throws() {
        TransService ts = new TransService();
        Assertions.assertThrows(TransException.class, () -> ts.trans(new UnregisteredRepoDto(1L)));
    }

    /** 引用了不存在的翻译仓库名的 DTO */
    static class DanglingDto {
        private Long id;
        @Trans(trans = "noSuchRepo")
        private String name;

        DanglingDto(Long id) {
            this.id = id;
        }

        public Long getId() {
            return id;
        }

        public String getName() {
            return name;
        }
    }

    @Test
    void danglingRepoReference_throws() {
        TransService ts = new TransService();
        Assertions.assertThrows(TransException.class, () -> ts.trans(new DanglingDto(1L)));
    }

}
