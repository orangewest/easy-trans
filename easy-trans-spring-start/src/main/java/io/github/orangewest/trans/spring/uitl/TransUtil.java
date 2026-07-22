package io.github.orangewest.trans.spring.uitl;

import io.github.orangewest.trans.service.TransService;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

public class TransUtil implements ApplicationContextAware {

    private static ApplicationContext applicationContext;

    /**
     * 翻译工具：便捷的静态入口，直接委托给 Spring 容器中持有的 {@link TransService}。
     * 真正的适配器分发与翻译逻辑都在引擎 {@link TransService#trans(Object)} 中，本类不持有任何翻译逻辑。
     *
     * <p>reactor 解析器（{@code ReactorTransResolver}）仅在 reactor 位于 classpath 时由
     * {@code EasyTransAutoConfiguration} 经 {@code @ConditionalOnClass} 注入为 Spring Bean，并由
     * {@code EasyTransRegister} 注册进 {@code TransValueResolverFactory}；core 引擎与 TransUtil 均不静态
     * 引用 reactor，保证纯 MVC 应用打 GraalVM Native 镜像不会因缺少 reactor 而构建失败。
     *
     * @param result 需要翻译的对象或返回值
     * @return 翻译后的对象（未注入 TransService 时原样返回）
     */
    public static Object trans(Object result) {
        TransService service = TransServiceHolder.get();
        if (service != null) {
            return service.trans(result);
        }
        return result;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        TransUtil.applicationContext = applicationContext;
    }

    /**
     * 懒加载 {@link TransService}：首次调用时从 Spring 容器取 Bean 并缓存，
     * 不再在类加载时急切初始化（避免容器尚未注入 context 时的 NPE）。
     */
    static class TransServiceHolder {
        private static volatile TransService INSTANCE;

        static TransService get() {
            TransService instance = INSTANCE;
            if (instance != null) {
                return instance;
            }
            ApplicationContext ctx = applicationContext;
            if (ctx == null) {
                return null;
            }
            synchronized (TransServiceHolder.class) {
                if (INSTANCE == null) {
                    INSTANCE = ctx.getBean(TransService.class);
                }
                return INSTANCE;
            }
        }
    }

}
