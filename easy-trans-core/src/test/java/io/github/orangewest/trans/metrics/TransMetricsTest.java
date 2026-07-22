package io.github.orangewest.trans.metrics;

import io.github.orangewest.trans.annotation.Trans;
import io.github.orangewest.trans.annotation.TransRepo;
import io.github.orangewest.trans.dto.CityDto;
import io.github.orangewest.trans.repository.CityTransRepository;
import io.github.orangewest.trans.repository.TransContext;
import io.github.orangewest.trans.repository.TransRepository;
import io.github.orangewest.trans.repository.TransRepositoryFactory;
import io.github.orangewest.trans.service.TransService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
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

    /**
     * 录制型 {@link TransMetrics}：把每次 {@code startSpan} 连同其上下文与返回的 Span 句柄录制下来，
     * 便于黑盒断言三级链路、parent 引用关系与 depth。
     */
    static class RecordingMetrics implements TransMetrics {
        final List<RecordingMetrics.SpanRecord> spans = new ArrayList<>();

        @Override
        public Span startSpan(String operation, TransMetricContext context) {
            RecordingSpan span = new RecordingSpan(operation, context);
            SpanRecord record = new SpanRecord(operation, context, span);
            spans.add(record);
            return span;
        }

        SpanRecord findBySpan(Span span) {
            return spans.stream().filter(r -> r.span == span).findFirst().orElse(null);
        }

        List<RecordingMetrics.SpanRecord> findByOperation(String operation) {
            return spans.stream().filter(r -> operation.equals(r.operation)).collect(Collectors.toList());
        }

        static final class RecordingSpan implements Span {
            final String operation;
            final TransMetricContext context;
            final AtomicBoolean errored = new AtomicBoolean();
            final AtomicInteger ends = new AtomicInteger();

            RecordingSpan(String operation, TransMetricContext context) {
                this.operation = operation;
                this.context = context;
            }

            @Override
            public void setAttribute(String key, String value) {
                // no-op
            }

            @Override
            public void recordException(Throwable t) {
                errored.set(true);
            }

            @Override
            public void end() {
                ends.incrementAndGet();
            }
        }

        static final class SpanRecord {
            final String operation;
            final TransMetricContext context;
            final Span span;

            SpanRecord(String operation, TransMetricContext context, Span span) {
                this.operation = operation;
                this.context = context;
                this.span = span;
            }
        }
    }

    RecordingMetrics recording;

    @BeforeEach
    void setup() {
        TransRepositoryFactory.register(new NameRepo());
        TransRepositoryFactory.register(new CityTransRepository());
        recording = new RecordingMetrics();
        TransMetricsCollector.set(recording);
    }

    @AfterEach
    void reset() {
        TransMetricsCollector.set(new NoopTransMetrics());
    }

    @Test
    void records_three_level_span_chain() {
        TransService service = new TransService();
        MetricsDto dto = new MetricsDto(1L);
        service.trans(dto);

        assertEquals("name-1", dto.name);

        // translate（根，depth=0，parent=null）
        List<RecordingMetrics.SpanRecord> translates = recording.findByOperation(TransMetricsOperations.TRANSLATE);
        assertEquals(1, translates.size(), "应恰好一个 translate Span");
        RecordingMetrics.SpanRecord translate = translates.get(0);
        assertNull(translate.context.getParent(), "translate 为根，parent 应为 null");
        assertEquals(0, translate.context.getDepth(), "translate depth 应为 0");
        assertEquals(MetricsDto.class, translate.context.getTargetClass());

        // repository（depth=1，parent=translate）
        List<RecordingMetrics.SpanRecord> repos = recording.findByOperation(TransMetricsOperations.REPOSITORY);
        assertFalse(repos.isEmpty(), "应至少记录一个 repository Span");
        for (RecordingMetrics.SpanRecord repo : repos) {
            assertEquals(translate.span, repo.context.getParent(), "repository 的 parent 应为 translate Span");
            assertEquals(1, repo.context.getDepth(), "repository depth 应为 1");
            assertEquals("id", repo.context.getRepoName());
            assertEquals(NameRepo.class, repo.context.getRepositoryClass());
        }

        // field（depth=2，parent=repository）
        List<RecordingMetrics.SpanRecord> fields = recording.findByOperation(TransMetricsOperations.FIELD);
        assertFalse(fields.isEmpty(), "应至少记录一个 field Span");
        for (RecordingMetrics.SpanRecord field : fields) {
            RecordingMetrics.SpanRecord parentRepo = recording.findBySpan(field.context.getParent());
            assertNotNull(parentRepo, "field 的 parent 应为已录制的 Span");
            assertEquals(TransMetricsOperations.REPOSITORY, parentRepo.operation, "field 的 parent 应为 repository Span");
            assertEquals(2, field.context.getDepth(), "field depth 应为 2");
            assertEquals("name", field.context.getFieldName());
        }
    }

    @Test
    void records_exception_on_failed_translation() {
        TransService service = new TransService();
        // 引用不存在的仓库：触发 TransException，应记录到 translate Span
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

        List<RecordingMetrics.SpanRecord> translates = recording.findByOperation(TransMetricsOperations.TRANSLATE);
        assertEquals(1, translates.size());
        RecordingMetrics.RecordingSpan translateSpan = (RecordingMetrics.RecordingSpan) translates.get(0).span;
        assertTrue(translateSpan.errored.get(), "失败的翻译应记录异常到 translate Span");
        assertEquals(1, translateSpan.ends.get(), "translate Span 仍应被 end");
    }

    @Test
    void records_nested_chain_with_increasing_depth() {
        TransService service = new TransService();
        service.trans(new CityDto(2L));

        // 嵌套翻译应产生 depth >= 3 的 repository Span（area→city→province 多级）
        List<RecordingMetrics.SpanRecord> nestedRepos = recording.findByOperation(TransMetricsOperations.REPOSITORY).stream()
                .filter(r -> r.context.getDepth() >= 3)
                .collect(Collectors.toList());
        assertFalse(nestedRepos.isEmpty(), "嵌套翻译应产生 depth>=3 的 repository Span");

        for (RecordingMetrics.SpanRecord nested : nestedRepos) {
            // 嵌套 repository 的 parent 应为 field Span（上层字段）
            RecordingMetrics.SpanRecord parent = recording.findBySpan(nested.context.getParent());
            assertNotNull(parent, "嵌套 repository 的 parent 应被录制");
            assertEquals(TransMetricsOperations.FIELD, parent.operation, "嵌套 repository 的 parent 应为 field Span");

            // 沿 parent 链向上，depth 应严格递减直到 translate（depth=0）
            int prevDepth = nested.context.getDepth();
            RecordingMetrics.SpanRecord cur = parent;
            while (cur != null) {
                assertTrue(cur.context.getDepth() < prevDepth,
                        "depth 应随层级递增（父 < 子）：parent=" + cur.context.getDepth() + " child=" + prevDepth);
                prevDepth = cur.context.getDepth();
                cur = recording.findBySpan(cur.context.getParent());
            }
            assertEquals(0, prevDepth, "链路顶端应为 translate（depth=0）");
        }
    }


}
