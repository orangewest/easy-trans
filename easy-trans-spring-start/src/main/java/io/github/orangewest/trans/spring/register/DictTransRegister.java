package io.github.orangewest.trans.spring.register;

import io.github.orangewest.trans.repository.TransRepositoryFactory;
import io.github.orangewest.trans.repository.dict.DictLoader;
import io.github.orangewest.trans.repository.dict.DictTransRepository;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

public class DictTransRegister implements InitializingBean, ApplicationContextAware {

    private ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Override
    public void afterPropertiesSet() {
        ObjectProvider<DictLoader> dictLoaderProvider = applicationContext.getBeanProvider(DictLoader.class);
        DictLoader dictLoader = dictLoaderProvider.getIfAvailable();
        if (dictLoader != null) {
            TransRepositoryFactory.register(new DictTransRepository(dictLoader));
        }
    }

}
