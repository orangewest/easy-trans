package io.github.orangewest.trans.propagation;

import io.github.orangewest.trans.annotation.Trans;
import io.github.orangewest.trans.repository.TransContext;
import io.github.orangewest.trans.repository.TransRepository;
import io.github.orangewest.trans.repository.TransRepositoryFactory;
import io.github.orangewest.trans.service.TransService;
import org.junit.jupiter.api.AfterEach;
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
 * 并行取数的上下文传播（TransContextPropagator + TransContextPropagatorFactory）：
 * <ul>
 *   <li>未注册 → 并行虚拟线程读不到调用线程 ThreadLocal（历史行为，对照）；</li>
 *   <li>经工厂注册 → 并行虚拟线程能恢复调用线程上下文；</li>
 *   <li>注册多个 → 工厂组合为 CompositeContextPropagator，按注册顺序 capture/restore、逆序 clear、下标对齐。</li>
 * </ul>
 */
class TransContextPropagatorTest {

    /** 被传播的示例上下文（模拟 SecurityContext / MDC / 租户 ID 等）。 */
    static final ThreadLocal<String> CTX = new ThreadLocal<>();

    /** 记录每个仓库在其执行线程上观察到的上下文值。 */
    static final List<String> SEEN = Collections.synchronizedList(new ArrayList<>());

    static class RepoA implements TransRepository<Long, String> {
        @Override
        public Map<Long, String> getTransValueMap(List<Long> transValues, TransContext context) {
            SEEN.add(CTX.get());
            Map<Long, String> m = new HashMap<>();
            transValues.forEach(k -> m.put(k, "a" + k));
            return m;
        }
    }

    static class RepoB implements TransRepository<Long, String> {
        @Override
        public Map<Long, String> getTransValueMap(List<Long> transValues, TransContext context) {
            SEEN.add(CTX.get());
            Map<Long, String> m = new HashMap<>();
            transValues.forEach(k -> m.put(k, "b" + k));
            return m;
        }
    }

    /** 两个不同仓库 → 2 个分组 → 触发并行虚拟线程执行。 */
    static class Dto {
        private Long aId;
        @Trans(trans = "aId", using = RepoA.class)
        private String aName;
        private Long bId;
        @Trans(trans = "bId", using = RepoB.class)
        private String bName;

        Dto(Long aId, Long bId) {
            this.aId = aId;
            this.bId = bId;
        }

        public String getAName() {
            return aName;
        }

        public String getBName() {
            return bName;
        }
    }

    static class CtxPropagator implements TransContextPropagator {
        @Override
        public Object capture() {
            return CTX.get();
        }

        @Override
        public void restore(Object snapshot) {
            CTX.set((String) snapshot);
        }

        @Override
        public void clear() {
            CTX.remove();
        }
    }

    @BeforeAll
    static void before() {
        TransRepositoryFactory.register(new RepoA());
        TransRepositoryFactory.register(new RepoB());
    }

    @BeforeEach
    void reset() {
        SEEN.clear();
        CTX.remove();
        TransContextPropagatorFactory.clear();
    }

    @AfterEach
    void cleanup() {
        TransContextPropagatorFactory.clear();
        CTX.remove();
    }

    @Test
    void withoutPropagator_contextLostOnParallelThreads() {
        CTX.set("tenant-1");
        TransService service = new TransService(); // 工厂为空 → NOOP

        service.trans(new Dto(1L, 2L));

        // 并行虚拟线程不继承调用线程 ThreadLocal → 两个仓库都看不到上下文
        Assertions.assertEquals(2, SEEN.size());
        Assertions.assertTrue(SEEN.stream().allMatch(v -> v == null), "无传播器时并行线程应读不到上下文");
    }

    @Test
    void withPropagator_contextPropagatedToParallelThreads() {
        CTX.set("tenant-42");
        TransContextPropagatorFactory.register(new CtxPropagator());
        TransService service = new TransService();

        Dto dto = new Dto(1L, 2L);
        service.trans(dto);

        Assertions.assertEquals(2, SEEN.size());
        Assertions.assertTrue(SEEN.stream().allMatch("tenant-42"::equals), "注册传播器后并行线程应恢复上下文");
        Assertions.assertEquals("a1", dto.getAName());
        Assertions.assertEquals("b2", dto.getBName());
    }

    @Test
    void factory_emptyReturnsNoop_singleReturnsItself() {
        Assertions.assertSame(TransContextPropagator.NOOP, TransContextPropagatorFactory.get());

        CtxPropagator only = new CtxPropagator();
        TransContextPropagatorFactory.register(only);
        Assertions.assertSame(only, TransContextPropagatorFactory.get());
    }

    @Test
    void factory_multipleComposedInOrder() {
        List<String> log = Collections.synchronizedList(new ArrayList<>());
        TransContextPropagatorFactory.register(recording("p1", log));
        TransContextPropagatorFactory.register(recording("p2", log));

        TransContextPropagator combined = TransContextPropagatorFactory.get();
        Object snapshot = combined.capture();
        combined.restore(snapshot);
        combined.clear();

        Assertions.assertEquals(
                List.of("p1.capture", "p2.capture",
                        "p1.restore:p1-snap", "p2.restore:p2-snap",
                        "p2.clear", "p1.clear"),
                log);
    }

    /** 记录调用轨迹的传播器：capture 返回专属快照，restore 记录收到的快照以验证下标对齐。 */
    private static TransContextPropagator recording(String name, List<String> log) {
        return new TransContextPropagator() {
            @Override
            public Object capture() {
                log.add(name + ".capture");
                return name + "-snap";
            }

            @Override
            public void restore(Object snapshot) {
                log.add(name + ".restore:" + snapshot);
            }

            @Override
            public void clear() {
                log.add(name + ".clear");
            }
        };
    }
}
