package io.github.orangewest.trans.spring.metrics;

import io.github.orangewest.trans.metrics.TransMetrics;
import io.github.orangewest.trans.metrics.TransMetricContext;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;

/**
 * 将框架的 {@link TransMetrics} 桥接到 Micrometer Observation。
 * <p>
 * 仅在 classpath 存在 Micrometer Observation 时通过自动配置装配
 * （{@code micrometer-core} 在 spring-start 中为 optional 依赖，其传递依赖包含 {@code micrometer-observation}）。
 * 借助 Spring Boot 自动注册的 {@code TimerObservationHandler}，Observation 会自动以 Timer 形式暴露为
 * {@code easytrans.translate} / {@code easytrans.repository} / {@code easytrans.field}（并附带低基数 tag）；
 * 若运行环境存在 Tracing 基础设施，Observation 还会天然具备链路追踪能力。
 *
 * <p>tag 映射策略（低基数才入 tag，防基数爆炸）：
 * <ul>
 *     <li>low cardinality：{@code operation}、{@code repo}（repoName）、{@code success}（!errored）、{@code depth}</li>
 *     <li>high cardinality（默认不入 tag）：{@code fieldName}、{@code targetClass}、{@code repositoryClass}；
 *     如需下钻，后端可在 {@link Span#setAttribute(String, String)} 中显式补充</li>
 * </ul>
 */
public class TransMetricsMicrometer implements TransMetrics {

    private static final String OBSERVATION_PREFIX = "easytrans.";

    private final ObservationRegistry observationRegistry;

    public TransMetricsMicrometer(ObservationRegistry observationRegistry) {
        this.observationRegistry = observationRegistry;
    }

    @Override
    public Span startSpan(String operation, TransMetricContext context) {
        Observation observation = Observation.createNotStarted(OBSERVATION_PREFIX + operation, observationRegistry);
        if (context.getParent() instanceof ObservationSample parentSample) {
            observation = observation.parentObservation(parentSample.observation);
        }
        observation.start();
        observation.lowCardinalityKeyValue("operation", operation);
        observation.lowCardinalityKeyValue("depth", String.valueOf(context.getDepth()));
        if (context.getRepoName() != null) {
            observation.lowCardinalityKeyValue("repo", context.getRepoName());
        }
        return new ObservationSample(observation);
    }

    @Override
    public void increment(String operation, TransMetricContext context, long n) {
        // 计数器本期仅设计预留，引擎未接埋点；此处空操作。
    }

    private static final class ObservationSample implements Span {

        private final Observation observation;
        private boolean errored = false;

        private ObservationSample(Observation observation) {
            this.observation = observation;
        }

        @Override
        public void setAttribute(String key, String value) {
            // 经 setAttribute 显式补充的属性按设计作为 high-cardinality 维度纳入，
            // 避免误用低基数 tag 导致基数爆炸。
            observation.highCardinalityKeyValue(key, value);
        }

        @Override
        public void recordException(Throwable t) {
            errored = true;
            observation.error(t);
        }

        @Override
        public void end() {
            observation.lowCardinalityKeyValue("success", String.valueOf(!errored));
            observation.stop();
        }
    }

}
