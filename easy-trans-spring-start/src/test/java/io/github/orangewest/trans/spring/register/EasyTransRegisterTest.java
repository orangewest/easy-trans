package io.github.orangewest.trans.spring.register;

import io.github.orangewest.trans.annotation.Trans;
import io.github.orangewest.trans.annotation.TransRepo;
import io.github.orangewest.trans.metrics.NoopTransMetrics;
import io.github.orangewest.trans.metrics.TransMetricContext;
import io.github.orangewest.trans.metrics.TransMetrics;
import io.github.orangewest.trans.metrics.TransMetricsCollector;
import io.github.orangewest.trans.metrics.TransMetricsOperations;
import io.github.orangewest.trans.repository.TransContext;
import io.github.orangewest.trans.repository.TransRepository;
import io.github.orangewest.trans.repository.TransRepositoryFactory;
import io.github.orangewest.trans.service.TransService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EasyTransRegisterTest {

    /** 录制型自定义后端，用于验证「经 set 注入」与「经 Spring BeanPostProcessor 自动注册」两种路径。 */
    static class RecordingBackend implements TransMetrics {
        final AtomicInteger spanStarts = new AtomicInteger();

        @Override
        public Span startSpan(String operation, TransMetricContext context) {
            spanStarts.incrementAndGet();
            return new Span() {
                @Override
                public void setAttribute(String key, String value) {
                }

                @Override
                public void recordException(Throwable t) {
                }

                @Override
                public void end() {
                }
            };
        }

        @Override
        public void increment(String operation, TransMetricContext context, long n) {
        }
    }

    static class NameRepo implements TransRepository<Long, String> {
        @Override
        public Map<Long, String> getTransValueMap(List<Long> transValues, TransContext context) {
            Map<Long, String> map = new java.util.HashMap<>();
            for (Long v : transValues) {
                map.put(v, "name-" + v);
            }
            return map;
        }
    }

    static class Dto {
        @TransRepo(using = NameRepo.class)
        public Long id;

        @Trans(trans = "id")
        public String name;

        Dto(Long id) {
            this.id = id;
        }
    }

    @BeforeEach
    void setup() {
        TransRepositoryFactory.register(new NameRepo());
    }

    @AfterEach
    void reset() {
        TransMetricsCollector.set(new NoopTransMetrics());
    }

    @Test
    void custom_backend_is_auto_registered_by_bean_post_processor() {
        RecordingBackend backend = new RecordingBackend();
        new EasyTransRegister().postProcessAfterInitialization(backend, "customMetrics");

        assertSame(backend, TransMetricsCollector.get(),
                "自定义 TransMetrics bean 应被 EasyTransRegister 自动注册到 TransMetricsCollector");
    }

    @Test
    void auto_registered_custom_backend_is_invoked_during_translation() {
        RecordingBackend backend = new RecordingBackend();
        new EasyTransRegister().postProcessAfterInitialization(backend, "customMetrics");

        TransService service = new TransService();
        Dto dto = new Dto(1L);
        assertTrue(service.trans(dto));
        assertEquals("name-1", dto.name);

        assertTrue(backend.spanStarts.get() > 0, "自定义后端应在翻译时被实际调用（录制到 Span）");
    }

    @Test
    void backend_set_directly_is_invoked_during_translation() {
        RecordingBackend backend = new RecordingBackend();
        TransMetricsCollector.set(backend);

        TransService service = new TransService();
        assertTrue(service.trans(new Dto(2L)));

        assertTrue(backend.spanStarts.get() > 0, "经 set 注入的自定义后端应在翻译时被实际调用");
    }
}
