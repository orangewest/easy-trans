package io.github.orangewest.trans.spring.metrics;

import io.github.orangewest.trans.metrics.TransMetrics;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;

/**
 * 将框架的 {@link TransMetrics} 桥接到 Micrometer Observation。
 * <p>
 * 仅在 classpath 存在 Micrometer Observation 时通过自动配置装配
 * （{@code micrometer-core} 在 spring-start 中为 optional 依赖，其传递依赖包含 {@code micrometer-observation}）。
 * 借助 Spring Boot 自动注册的 {@code TimerObservationHandler}，Observation 会自动以 Timer 形式暴露为
 * {@code easytrans.translate} / {@code easytrans.repository}（并附带 {@code repo}、{@code success} 等 tag）；
 * 若运行环境存在 Tracing 基础设施，Observation 还会天然具备链路追踪能力。
 */
public class TransMetricsMicrometer implements TransMetrics {

    private final ObservationRegistry observationRegistry;

    public TransMetricsMicrometer(ObservationRegistry observationRegistry) {
        this.observationRegistry = observationRegistry;
    }

    @Override
    public Sample startTranslate() {
        return new ObservationSample(Observation.start("easytrans.translate", observationRegistry));
    }

    @Override
    public Sample startRepository(String repoName) {
        Observation observation = Observation.start("easytrans.repository", observationRegistry)
                .lowCardinalityKeyValue("repo", repoName);
        return new ObservationSample(observation);
    }

    private static final class ObservationSample implements Sample {

        private final Observation observation;
        private boolean errored = false;

        private ObservationSample(Observation observation) {
            this.observation = observation;
        }

        @Override
        public void error(Throwable t) {
            errored = true;
            observation.error(t);
        }

        @Override
        public void stop() {
            observation.lowCardinalityKeyValue("success", String.valueOf(!errored));
            observation.stop();
        }
    }

}
