package io.github.orangewest.trans.metrics;

import org.junit.jupiter.api.Test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class TransMetricContextTest {

    @Target(ElementType.FIELD)
    @Retention(RetentionPolicy.RUNTIME)
    @interface MyAnno {
    }

    /** 用作 parent 的占位 Span。 */
    private static final TransMetrics.Span DUMMY_PARENT = new TransMetrics.Span() {
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

    static class AnnotatedHolder {
        @MyAnno
        String field;
    }

    @Test
    void builder_carries_all_eight_semantic_fields() throws Exception {
        MyAnno anno = AnnotatedHolder.class.getDeclaredField("field").getAnnotation(MyAnno.class);

        TransMetricContext ctx = TransMetricContext.builder(TransMetricsOperations.FIELD)
                .parent(DUMMY_PARENT)
                .depth(3)
                .targetClass(String.class)
                .fieldName("name")
                .repoName("repoField")
                .repositoryClass(Runnable.class)
                .annotation(anno)
                .build();

        assertEquals(TransMetricsOperations.FIELD, ctx.getOperation());
        assertEquals(3, ctx.getDepth());
        assertSame(DUMMY_PARENT, ctx.getParent());
        assertEquals(String.class, ctx.getTargetClass());
        assertEquals("name", ctx.getFieldName());
        assertEquals("repoField", ctx.getRepoName());
        assertEquals(Runnable.class, ctx.getRepositoryClass());
        assertSame(anno, ctx.getAnnotation());
    }

    @Test
    void builder_defaults_absent_fields_to_null() {
        TransMetricContext ctx = TransMetricContext.builder(TransMetricsOperations.TRANSLATE)
                .depth(0)
                .build();

        assertEquals(TransMetricsOperations.TRANSLATE, ctx.getOperation());
        assertEquals(0, ctx.getDepth());
        assertNull(ctx.getParent());
        assertNull(ctx.getTargetClass());
        assertNull(ctx.getFieldName());
        assertNull(ctx.getRepoName());
        assertNull(ctx.getRepositoryClass());
        assertNull(ctx.getAnnotation());
    }

    @Test
    void instances_are_immutable_and_builder_is_reusable_separately() {
        TransMetricContext.Builder builder = TransMetricContext.builder(TransMetricsOperations.REPOSITORY)
                .repoName("a")
                .depth(1);
        TransMetricContext first = builder.build();
        // 继续用同一个 builder 构造另一个实例，不应影响已 build 的实例
        TransMetricContext second = builder.repoName("b").depth(2).build();

        assertEquals("a", first.getRepoName());
        assertEquals(1, first.getDepth());
        assertEquals("b", second.getRepoName());
        assertEquals(2, second.getDepth());
    }
}
