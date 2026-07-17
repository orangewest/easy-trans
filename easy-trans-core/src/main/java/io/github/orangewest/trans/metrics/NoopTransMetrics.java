package io.github.orangewest.trans.metrics;

/**
 * {@link TransMetrics} 的空实现，未接入任何监控系统时使用，所有方法为空操作。
 */
public class NoopTransMetrics implements TransMetrics {

    @Override
    public void recordTranslate(long durationNanos, boolean success) {
        // no-op
    }

    @Override
    public void recordRepository(String repoName, long durationNanos, boolean success) {
        // no-op
    }

}
