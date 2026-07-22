package io.github.orangewest.trans.metrics;

/**
 * {@link TransMetrics} 的空实现，未接入任何监控系统时使用，所有方法为空操作。
 * <p>
 * 忽略 {@code parent} / {@code depth} / {@code context}，返回共享 NoopSpan，保证无监控系统时零开销、无 NPE。
 */
public class NoopTransMetrics implements TransMetrics {

    private static final Span NOOP_SPAN = new Span() {
        @Override
        public void setAttribute(String key, String value) {
            // no-op
        }

        @Override
        public void recordException(Throwable t) {
            // no-op
        }

        @Override
        public void end() {
            // no-op
        }
    };

    @Override
    public Span startSpan(String operation, TransMetricContext context) {
        return NOOP_SPAN;
    }

}
