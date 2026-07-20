package io.github.orangewest.trans.metrics;

/**
 * {@link TransMetrics} 的空实现，未接入任何监控系统时使用，所有方法为空操作。
 */
public class NoopTransMetrics implements TransMetrics {

    private static final Sample NOOP_SAMPLE = new Sample() {
        @Override
        public void error(Throwable t) {
            // no-op
        }

        @Override
        public void stop() {
            // no-op
        }
    };

    @Override
    public Sample startTranslate() {
        return NOOP_SAMPLE;
    }

    @Override
    public Sample startRepository(String repoName) {
        return NOOP_SAMPLE;
    }

}
