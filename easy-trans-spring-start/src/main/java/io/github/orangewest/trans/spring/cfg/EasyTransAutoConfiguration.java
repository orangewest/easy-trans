package io.github.orangewest.trans.spring.cfg;

import io.github.orangewest.trans.metrics.TransMetrics;
import io.github.orangewest.trans.metrics.TransMetricsCollector;
import io.github.orangewest.trans.repository.dict.DictLoader;
import io.github.orangewest.trans.repository.dict.DictTransRepository;
import io.github.orangewest.trans.service.TransService;
import io.github.orangewest.trans.spring.aop.AutoTransAspect;
import io.github.orangewest.trans.spring.metrics.TransMetricsMicrometer;
import io.github.orangewest.trans.spring.register.EasyTransRegister;
import io.github.orangewest.trans.repository.enumdict.EnumTransRepository;
import io.github.orangewest.trans.spring.resolver.ReactorTransResolver;
import io.github.orangewest.trans.spring.uitl.TransUtil;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EasyTransAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public TransService transService() {
        return new TransService();
    }

    @Bean
    @ConditionalOnBean(DictLoader.class)
    public DictTransRepository dictTransRepository(DictLoader dictLoader) {
        return new DictTransRepository(dictLoader);
    }

    @Bean
    public static EasyTransRegister easyTransRegister() {
        return new EasyTransRegister();
    }

    @Bean
    public AutoTransAspect autoTransAspect() {
        return new AutoTransAspect();
    }

    @Bean
    public TransUtil transUtil() {
        return new TransUtil();
    }

    @Bean
    public EnumTransRepository enumTransRepository() {
        return new EnumTransRepository();
    }

    /**
     * 响应式翻译解析器（{@code io.github.orangewest.trans.spring.resolver.ReactorTransResolver}）：仅当 classpath
     * 存在 reactor（WebFlux 应用）时注入为 Spring Bean，由
     * {@link io.github.orangewest.trans.spring.register.EasyTransRegister} 自动注册进
     * {@code TransValueResolverFactory}。纯 MVC 应用不含 reactor，该 Bean 不创建，框架退化为
     * 只处理同步对象与 CompletableFuture。
     */
    @Bean
    @ConditionalOnClass(name = "reactor.core.publisher.Mono")
    public ReactorTransResolver reactorTransResolver() {
        return new ReactorTransResolver();
    }

    /**
     * 当 classpath 存在 Micrometer Observation 且容器中有 ObservationRegistry 时，将翻译指标桥接到 Micrometer。
     * micrometer-core 为 optional 依赖（其传递依赖包含 micrometer-observation），未引入时该配置不生效，
     * 框架退化为无指标（零开销）。
     */
    @Configuration
    @ConditionalOnClass(name = "io.micrometer.observation.ObservationRegistry")
    static class MetricsConfiguration {

        @Bean
        @ConditionalOnBean(io.micrometer.observation.ObservationRegistry.class)
        public TransMetrics transMetrics(io.micrometer.observation.ObservationRegistry observationRegistry) {
            TransMetricsMicrometer transMetrics = new TransMetricsMicrometer(observationRegistry);
            TransMetricsCollector.set(transMetrics);
            return transMetrics;
        }
    }

}
