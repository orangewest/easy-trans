package io.github.orangewest.trans.spring.register;

import io.github.orangewest.trans.metrics.TransMetrics;
import io.github.orangewest.trans.metrics.TransMetricsCollector;
import io.github.orangewest.trans.repository.TransRepository;
import io.github.orangewest.trans.repository.TransRepositoryFactory;
import io.github.orangewest.trans.resolver.TransObjResolver;
import io.github.orangewest.trans.resolver.TransObjResolverFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;

public class EasyTransRegister implements BeanPostProcessor {

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if (bean instanceof TransRepository) {
            TransRepositoryFactory.register((TransRepository<?, ?>) bean);
        } else if (bean instanceof TransObjResolver) {
            TransObjResolverFactory.register((TransObjResolver) bean);
        } else if (bean instanceof TransMetrics) {
            // 用户自定义 TransMetrics 后端经 Spring 容器自动注册，无需手动 TransMetricsCollector.set(...)
            TransMetricsCollector.set((TransMetrics) bean);
        }
        return bean;
    }

}
