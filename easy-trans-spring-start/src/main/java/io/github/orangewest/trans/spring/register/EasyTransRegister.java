package io.github.orangewest.trans.spring.register;

import io.github.orangewest.trans.resolver.TransValueResolver;
import io.github.orangewest.trans.resolver.TransValueResolverFactory;
import io.github.orangewest.trans.metrics.TransMetrics;
import io.github.orangewest.trans.metrics.TransMetricsCollector;
import io.github.orangewest.trans.repository.TransRepository;
import io.github.orangewest.trans.repository.TransRepositoryFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;

public class EasyTransRegister implements BeanPostProcessor {

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if (bean instanceof TransRepository) {
            TransRepositoryFactory.register((TransRepository<?, ?>) bean);
        } else if (bean instanceof TransValueResolver) {
            // 用户自定义翻译值解析器（原 TransObjResolver 拆包、TransResultHandler 返回值处理统一为此接口）
            // 经 Spring 容器自动注册，无需手动 TransValueResolverFactory.register(...)
            TransValueResolverFactory.register((TransValueResolver) bean);
        } else if (bean instanceof TransMetrics) {
            // 用户自定义 TransMetrics 后端经 Spring 容器自动注册，无需手动 TransMetricsCollector.set(...)
            TransMetricsCollector.set((TransMetrics) bean);
        }
        return bean;
    }

}
