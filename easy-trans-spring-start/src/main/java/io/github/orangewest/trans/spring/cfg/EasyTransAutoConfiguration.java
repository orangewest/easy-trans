package io.github.orangewest.trans.spring.cfg;

import io.github.orangewest.trans.repository.dict.DictLoader;
import io.github.orangewest.trans.repository.dict.DictTransRepository;
import io.github.orangewest.trans.service.TransService;
import io.github.orangewest.trans.spring.aop.AutoTransAspect;
import io.github.orangewest.trans.spring.register.EasyTransRegister;
import io.github.orangewest.trans.spring.uitl.TransUtil;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EasyTransAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public TransService transService() {
        TransService transService = new TransService();
        transService.init();
        return transService;
    }

    @Bean
    @ConditionalOnBean(DictLoader.class)
    public DictTransRepository dictTransRepository(DictLoader dictLoader) {
        return new DictTransRepository(dictLoader);
    }

    @Bean
    public EasyTransRegister easyTransRegister() {
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

}
