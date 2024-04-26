package io.github.orangewest.trans.spring.uitl;


import io.github.orangewest.trans.service.TransService;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

public class TransUtil implements ApplicationContextAware {

    private static ApplicationContext applicationContext;

    /**
     * 翻译工具
     *
     * @param obj 需要翻译的对象
     * @return 是否翻译成功
     */
    public static boolean trans(Object obj) {
        return TransServiceHolder.get().trans(obj);
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        TransUtil.applicationContext = applicationContext;
    }

    static class TransServiceHolder {
        private static final TransService INSTANCE = applicationContext.getBean(TransService.class);

        public static TransService get() {
            return INSTANCE;
        }
    }

}
