package io.github.orangewest.trans.spring.cfg;

import io.github.orangewest.trans.metrics.NoopTransMetrics;
import io.github.orangewest.trans.metrics.TransMetrics;
import io.github.orangewest.trans.metrics.TransMetricsCollector;
import io.github.orangewest.trans.repository.dict.DictLoader;
import io.github.orangewest.trans.repository.dict.DictTransRepository;
import io.github.orangewest.trans.service.TransService;
import io.github.orangewest.trans.spring.aop.AutoTransAspect;
import io.github.orangewest.trans.spring.metrics.TransMetricsMicrometer;
import io.github.orangewest.trans.spring.propagation.MicrometerContextPropagator;
import io.github.orangewest.trans.spring.register.EasyTransRegister;
import io.github.orangewest.trans.repository.enumdict.EnumTransRepository;
import io.github.orangewest.trans.spring.resolver.ReactorTransResolver;
import io.github.orangewest.trans.spring.uitl.TransUtil;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(EasyTransProperties.class)
public class EasyTransAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public TransService transService(EasyTransProperties props) {
        TransService service = new TransService();
        service.setParallelRepoGroups(props.isParallelRepoGroups());
        service.setRepoBatchSize(props.getRepoBatchSize());
        return service;
    }

    /**
     * 基于 Micrometer {@code context-propagation} 的上下文传播桥接：仅当 classpath 存在
     * {@code io.micrometer.context.ContextSnapshotFactory} 且未显式关闭时装配（默认开）。
     * 它作为一个 {@link io.github.orangewest.trans.propagation.TransContextPropagator} Bean，经
     * {@link EasyTransRegister} 自动注册进 {@code TransContextPropagatorFactory}，一把抓走所有已注册
     * {@code ThreadLocalAccessor} 的上下文；引入后通常无需再写自定义传播器。
     */
    @Bean
    @ConditionalOnClass(name = "io.micrometer.context.ContextSnapshotFactory")
    @ConditionalOnProperty(prefix = "easy-trans.context-propagation.micrometer", name = "enabled", matchIfMissing = true)
    public MicrometerContextPropagator micrometerContextPropagator() {
        return new MicrometerContextPropagator();
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
     * micrometer-core 为 optional 依赖（其传递依赖包含 micrometer-observation），未引入时本 Bean 不装配，
     * 框架退化为无指标（零开销）。
     * <p>
     * 用 {@code ObjectProvider} 而非 {@code @ConditionalOnBean(ObservationRegistry.class)}：后者依赖 bean
     * 定义顺序，在本自动配置先于 Spring Boot 的 ObservationAutoConfiguration 时被判定为「无此 bean」而失效，
     * 导致 Micrometer 桥接永不装配。ObjectProvider 在 ObservationRegistry 可用时才装配，规避顺序问题。
     * <p>
     * 平铺安全性：对 {@code ObservationRegistry} 的引用仅出现在方法体与泛型形参中（擦除后形参为
     * {@code ObjectProvider}），配合方法级 {@code @ConditionalOnClass}（按类名字符串判定），未引入
     * micrometer-observation 时本方法被跳过、不触发类加载。
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnClass(name = "io.micrometer.observation.ObservationRegistry")
    public TransMetrics transMetrics(
            ObjectProvider<io.micrometer.observation.ObservationRegistry> observationRegistryProvider) {
        io.micrometer.observation.ObservationRegistry observationRegistry =
                observationRegistryProvider.getIfAvailable();
        if (observationRegistry != null) {
            TransMetricsMicrometer transMetrics = new TransMetricsMicrometer(observationRegistry);
            TransMetricsCollector.set(transMetrics);
            return transMetrics;
        }
        return new NoopTransMetrics();
    }

}
