package io.github.orangewest.trans.spring.metrics;

import io.github.orangewest.trans.metrics.TransMetrics;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

import java.util.concurrent.TimeUnit;

/**
 * 将框架的 {@link TransMetrics} 桥接到 Micrometer。
 * <p>
 * 仅在 classpath 存在 Micrometer 时通过自动配置装配（{@code micrometer-core} 在 spring-start 中为 optional 依赖）。
 * 指标：
 * <ul>
 *     <li>{@code easytrans.translate}：单次 trans() 调用耗时，tag {@code success}</li>
 *     <li>{@code easytrans.repository}：单个仓库翻译耗时，tag {@code repo}、{@code success}</li>
 * </ul>
 */
public class TransMetricsMicrometer implements TransMetrics {

    private final MeterRegistry registry;

    public TransMetricsMicrometer(MeterRegistry registry) {
        this.registry = registry;
    }

    @Override
    public void recordTranslate(long durationNanos, boolean success) {
        Timer.builder("easytrans.translate")
                .tag("success", String.valueOf(success))
                .description("easy-trans 翻译调用耗时")
                .register(registry)
                .record(durationNanos, TimeUnit.NANOSECONDS);
    }

    @Override
    public void recordRepository(String repoName, long durationNanos, boolean success) {
        Timer.builder("easytrans.repository")
                .tag("repo", repoName)
                .tag("success", String.valueOf(success))
                .description("单个翻译仓库耗时")
                .register(registry)
                .record(durationNanos, TimeUnit.NANOSECONDS);
    }

}
