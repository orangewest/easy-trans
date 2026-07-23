package io.github.orangewest.trans.service;

import io.github.orangewest.trans.annotation.Trans;
import io.github.orangewest.trans.annotation.TransNest;
import io.github.orangewest.trans.repository.TransContext;
import io.github.orangewest.trans.repository.TransRepository;
import io.github.orangewest.trans.repository.TransRepositoryFactory;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * P6：单次 trans() 调用内的仓库查询去重合并。
 * 级联（F13）后同一仓库会跨根层 / 嵌套类型被重复查询，键常重叠；开启合并后每个键在一次调用内只查一次。
 */
class TransRepoCoalescingTest {

    /** 记录每次 getTransValueMap 收到的全部键（跨所有调用累积），用于验证去重。 */
    static final List<Object> QUERIED_KEYS = Collections.synchronizedList(new ArrayList<>());

    static class CountingRepo implements TransRepository<Long, String> {
        @Override
        public Map<Long, String> getTransValueMap(List<Long> transValues, TransContext context) {
            QUERIED_KEYS.addAll(transValues);
            Map<Long, String> m = new HashMap<>();
            for (Long k : transValues) {
                m.put(k, "name" + k);
            }
            return m;
        }
    }

    static class Order {
        private Long userId;
        @Trans(trans = "userId", using = CountingRepo.class)
        private String userName;

        Order(Long userId) {
            this.userId = userId;
        }

        public String getUserName() {
            return userName;
        }
    }

    static class Report {
        private Long userId;
        @Trans(trans = "userId", using = CountingRepo.class)
        private String userName;
        @TransNest
        private List<Order> orders;

        Report(Long userId, List<Order> orders) {
            this.userId = userId;
            this.orders = orders;
        }

        public String getUserName() {
            return userName;
        }

        public List<Order> getOrders() {
            return orders;
        }
    }

    @BeforeAll
    static void before() {
        TransRepositoryFactory.register(new CountingRepo());
    }

    @BeforeEach
    void clear() {
        QUERIED_KEYS.clear();
    }

    private static long count(long key) {
        return QUERIED_KEYS.stream().filter(k -> k.equals(key)).count();
    }

    @Test
    void coalesced_sharedKeyQueriedOnce() {
        // root userId=1；两个 order 分别 userId=1（与 root 重叠）、userId=2
        Report report = new Report(1L, List.of(new Order(1L), new Order(2L)));
        TransService service = new TransService(); // 默认开启合并

        service.trans(report);

        // 键 1 被 root 与嵌套 Order 共用，合并后只查一次；键 2 只出现一次
        Assertions.assertEquals(1, count(1L), "重叠键 1 在一次调用内应只查一次");
        Assertions.assertEquals(1, count(2L), "键 2 应只查一次");
        // 结果仍正确
        Assertions.assertEquals("name1", report.getUserName());
        Assertions.assertEquals("name1", report.getOrders().get(0).getUserName());
        Assertions.assertEquals("name2", report.getOrders().get(1).getUserName());
    }

    @Test
    void disabled_sharedKeyQueriedPerLevel() {
        Report report = new Report(1L, List.of(new Order(1L)));
        TransService service = new TransService();
        service.setRepoCoalescing(false);

        service.trans(report);

        // 关闭合并：root 与嵌套 Order 各查一次键 1
        Assertions.assertEquals(2, count(1L), "关闭合并时重叠键应按层各查一次");
        Assertions.assertEquals("name1", report.getUserName());
        Assertions.assertEquals("name1", report.getOrders().get(0).getUserName());
    }
}
