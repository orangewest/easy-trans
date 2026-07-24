package io.github.orangewest.trans.spring.metrics;

import io.github.orangewest.trans.metrics.TransMetricContext;
import io.github.orangewest.trans.metrics.TransMetrics;
import io.github.orangewest.trans.metrics.TransMetricsOperations;
import io.micrometer.common.KeyValue;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationHandler;
import io.micrometer.observation.ObservationRegistry;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TransMetricsMicrometerTest {

    /** 录制 Observation 的 name 与低基数 tag，用于断言 Micrometer 映射。 */
    static class RecordingHandler implements ObservationHandler<Observation.Context> {
        final List<Recorded> recorded = new ArrayList<>();

        @Override
        public void onStart(Observation.Context context) {
        }

        @Override
        public void onError(Observation.Context context) {
        }

        @Override
        public void onStop(Observation.Context context) {
            recorded.add(new Recorded(context));
        }

        @Override
        public boolean supportsContext(Observation.Context context) {
            return true;
        }

        String valueOf(Recorded r, String key) {
            for (KeyValue kv : r.context.getLowCardinalityKeyValues()) {
                if (kv.getKey().equals(key)) {
                    return kv.getValue();
                }
            }
            return null;
        }
    }

    static class Recorded {
        final Observation.Context context;

        Recorded(Observation.Context context) {
            this.context = context;
        }
    }

    @Test
    void maps_operations_to_observation_names_with_low_cardinality_tags() {
        ObservationRegistry registry = ObservationRegistry.create();
        RecordingHandler handler = new RecordingHandler();
        registry.observationConfig().observationHandler(handler);

        TransMetricsMicrometer metrics = new TransMetricsMicrometer(registry);

        TransMetricContext repoCtx = TransMetricContext.builder(TransMetricsOperations.REPOSITORY)
                .repoName("sexRepo").targetClass(String.class).depth(1).build();
        TransMetrics.Span repoSpan = metrics.startSpan(TransMetricsOperations.REPOSITORY, repoCtx);
        repoSpan.end();

        // 1. Observation 名映射
        assertTrue(handler.recorded.stream().anyMatch(r -> "easytrans.repository".equals(r.context.getName())),
                "repository 应映射为 easytrans.repository");

        Recorded repo = handler.recorded.stream()
                .filter(r -> "easytrans.repository".equals(r.context.getName())).findFirst().orElseThrow();
        // 2. 低基数 tag：operation / repo / depth / success
        assertEquals("repository", handler.valueOf(repo, "operation"));
        assertEquals("sexRepo", handler.valueOf(repo, "repo"));
        assertEquals("1", handler.valueOf(repo, "depth"));
        assertEquals("true", handler.valueOf(repo, "success"));

        // 3. 高基数（targetClass）默认不进低基数 tag
        assertFalse(containsKey(repo, "targetClass"),
                "高基数 targetClass 默认不应进低基数 tag");
    }

    @Test
    void records_failure_via_success_tag() {
        ObservationRegistry registry = ObservationRegistry.create();
        RecordingHandler handler = new RecordingHandler();
        registry.observationConfig().observationHandler(handler);

        TransMetricsMicrometer metrics = new TransMetricsMicrometer(registry);
        TransMetricContext ctx = TransMetricContext.builder(TransMetricsOperations.TRANSLATE)
                .targetClass(String.class).depth(0).build();
        TransMetrics.Span span = metrics.startSpan(TransMetricsOperations.TRANSLATE, ctx);
        span.recordException(new RuntimeException("boom"));
        span.end();

        Recorded translate = handler.recorded.stream()
                .filter(r -> "easytrans.translate".equals(r.context.getName())).findFirst().orElseThrow();
        assertEquals("translate", handler.valueOf(translate, "operation"));
        assertEquals("false", handler.valueOf(translate, "success"), "异常应记录 success=false");
    }

    private static boolean containsKey(Recorded recorded, String key) {
        for (KeyValue kv : recorded.context.getLowCardinalityKeyValues()) {
            if (kv.getKey().equals(key)) {
                return true;
            }
        }
        return false;
    }

    @Test
    void setAttribute_goes_to_high_cardinality_not_low() {
        ObservationRegistry registry = ObservationRegistry.create();
        RecordingHandler handler = new RecordingHandler();
        registry.observationConfig().observationHandler(handler);

        TransMetricsMicrometer metrics = new TransMetricsMicrometer(registry);
        TransMetricContext ctx = TransMetricContext.builder(TransMetricsOperations.REPOSITORY)
                .repoName("sexRepo").targetClass(String.class).depth(2).build();
        TransMetrics.Span span = metrics.startSpan(TransMetricsOperations.REPOSITORY, ctx);
        // 经 setAttribute 显式开启高基数维度
        span.setAttribute("fieldName", "sexName");
        span.end();

        Recorded repo = handler.recorded.stream()
                .filter(r -> "easytrans.repository".equals(r.context.getName())).findFirst().orElseThrow();
        // 高基数维度不应进入低基数 tag
        assertFalse(containsKey(repo, "fieldName"),
                "经 setAttribute 补充的高基数维度不应进低基数 tag");
        // 应进入高基数 keyValues
        assertTrue(containsHighCardinalityKey(repo, "fieldName"),
                "经 setAttribute 补充的高基数维度应进高基数 keyValues");
    }

    private static boolean containsHighCardinalityKey(Recorded recorded, String key) {
        for (KeyValue kv : recorded.context.getHighCardinalityKeyValues()) {
            if (kv.getKey().equals(key)) {
                return true;
            }
        }
        return false;
    }
}
