package io.github.orangewest.trans.metrics;

/**
 * 翻译指标收集器（静态持有）。
 * <p>
 * 与 {@code TransRepositoryFactory} 类似的静态注册模式：引擎通过 {@link #get()} 获取当前指标实现，
 * 集成层（spring-start）在启动时通过 {@link #set(TransMetrics)} 注入具体实现（如 Micrometer 桥接）。
 * 未设置时使用 {@link NoopTransMetrics}，保证无监控系统时零开销。
 */
public final class TransMetricsCollector {

    private static volatile TransMetrics metrics = new NoopTransMetrics();

    private TransMetricsCollector() {
    }

    public static void set(TransMetrics transMetrics) {
        if (transMetrics != null) {
            metrics = transMetrics;
        }
    }

    public static TransMetrics get() {
        return metrics;
    }

}
