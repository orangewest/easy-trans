package io.github.orangewest.trans.spring.cfg;

import io.github.orangewest.trans.repository.dict.DictLoader;
import io.github.orangewest.trans.service.TransService;
import io.github.orangewest.trans.spring.aop.AutoTransAspect;
import io.github.orangewest.trans.spring.register.DictTransRegister;
import io.github.orangewest.trans.spring.register.EasyTransRegister;
import io.github.orangewest.trans.spring.uitl.TransUtil;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EasyTransAutoConfiguration {

    @Bean
    public TransService transService() {
        TransService transService = new TransService();
        transService.init();
        return transService;
    }

    @Bean
    @ConditionalOnBean(DictLoader.class)
    public DictTransRegister dictTransRegister() {
        return new DictTransRegister();
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
