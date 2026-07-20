package io.github.orangewest.trans.metrics;

import io.github.orangewest.trans.annotation.Trans;
import io.github.orangewest.trans.annotation.TransRepo;
import io.github.orangewest.trans.repository.TransContext;
import io.github.orangewest.trans.repository.TransRepository;
import io.github.orangewest.trans.repository.TransRepositoryFactory;
import io.github.orangewest.trans.service.TransService;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TransMetricsTest {

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

    static class MetricsDto {
        @TransRepo(using = NameRepo.class)
        public Long id;

        @Trans(trans = "id")
        public String name;

        MetricsDto(Long id) {
            this.id = id;
        }
    }

    static class RecordingMetrics implements TransMetrics {
        final AtomicInteger translateCalls = new AtomicInteger();
        final AtomicInteger repoCalls = new AtomicInteger();
        final AtomicReference<String> lastRepo = new AtomicReference<>();

        @Override
        public void recordTranslate(long durationNanos, boolean success) {
            translateCalls.incrementAndGet();
        }

        @Override
        public void recordRepository(String repoName, long durationNanos, boolean success) {
            repoCalls.incrementAndGet();
            lastRepo.set(repoName);
        }
    }

    static RecordingMetrics recording;

    @BeforeAll
    static void setup() {
        TransRepositoryFactory.register(new NameRepo());
        recording = new RecordingMetrics();
        TransMetricsCollector.set(recording);
    }

    @AfterAll
    static void reset() {
        TransMetricsCollector.set(new NoopTransMetrics());
    }

    @Test
    void metrics_are_recorded_on_translation() {
        int before = recording.translateCalls.get();
        TransService service = new TransService();
        MetricsDto dto = new MetricsDto(1L);
        boolean result = service.trans(dto);

        assertTrue(result);
        assertEquals("name-1", dto.name);
        assertTrue(recording.translateCalls.get() > before, "recordTranslate 应被调用一次");
        assertEquals(1, recording.repoCalls.get(), "recordRepository 应被调用一次");
        assertEquals("id", recording.lastRepo.get(), "仓库名应为 @TransRepo 字段名");
    }

    @Test
    void metrics_record_failure_on_exception() {
        int before = recording.translateCalls.get();
        TransService service = new TransService();
        // 引用不存在的仓库：触发 TransException，应记录 success=false
        class NoRepoDto {
            @Trans(trans = "missing")
            public String name;
        }
        boolean threw = false;
        try {
            service.trans(new NoRepoDto());
        } catch (Throwable t) {
            threw = true;
        }
        assertTrue(threw, "应抛出 TransException");
        assertTrue(recording.translateCalls.get() > before, "失败的翻译也应被记录");
    }
}
