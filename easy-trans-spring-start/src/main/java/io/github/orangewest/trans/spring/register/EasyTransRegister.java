package io.github.orangewest.trans.spring.register;

import io.github.orangewest.trans.resolver.TransValueResolver;
import io.github.orangewest.trans.resolver.TransValueResolverFactory;
import io.github.orangewest.trans.metrics.TransMetrics;
import io.github.orangewest.trans.metrics.TransMetricsCollector;
import io.github.orangewest.trans.repository.TransRepository;
import io.github.orangewest.trans.repository.TransRepositoryFactory;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;

public class EasyTransRegister implements BeanPostProcessor {

    @Override
    public Object postProcessAfterInitialization(@NonNull Object bean, @NonNull String beanName) throws BeansException {
        switch (bean) {
            case TransRepository<?,?> transRepository -> TransRepositoryFactory.register(transRepository);
            case TransValueResolver resolver ->
                // 用户自定义翻译值解析器（原 TransObjResolver 拆包、TransResultHandler 返回值处理统一为此接口）
                // 经 Spring 容器自动注册，无需手动 TransValueResolverFactory.register(...)
                TransValueResolverFactory.register(resolver);
            case TransMetrics transMetrics ->
                // 用户自定义 TransMetrics 后端经 Spring 容器自动注册，无需手动 TransMetricsCollector.set(...)
                TransMetricsCollector.set(transMetrics);
            default -> {
            }
        }
        return bean;
    }

}
