package io.github.orangewest.trans.spring.uitl;


import io.github.orangewest.trans.service.TransService;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import java.lang.reflect.Method;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;
import java.util.function.Function;

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

    /**
     * 翻译方法返回值。对于异步/响应式包装类型（CompletableFuture、Mono、Flux 等），
     * 会在其结果就绪后再执行翻译，而不是对包装对象本身翻译（那样会静默失效）。
     *
     * @param result 方法返回值
     * @return 原值，或包裹了翻译逻辑的异步/响应式对象
     */
    public static Object transResult(Object result) {
        return transResult(result, TransUtil::trans);
    }

    /**
     * 内部方法，允许注入 translator，便于单元测试。
     */
    static Object transResult(Object result, Consumer<Object> translator) {
        if (result == null) {
            return null;
        }
        // 异步返回：CompletableFuture 及其实现的 CompletionStage
        if (result instanceof CompletionStage) {
            return ((CompletionStage<Object>) result).thenApply(v -> {
                translator.accept(v);
                return v;
            });
        }
        // 响应式返回（Project Reactor）：仅在 classpath 存在时通过反射处理，避免引入硬依赖
        Object reactorResult = transReactor(result, translator);
        if (reactorResult != null) {
            return reactorResult;
        }
        // 同步返回（含 Result 等包装对象，由 trans 内部的解析器拆包）
        translator.accept(result);
        return result;
    }

    private static Object transReactor(Object result, Consumer<Object> translator) {
        if (!result.getClass().getName().startsWith("reactor.core.publisher.")) {
            return null;
        }
        try {
            Method map = result.getClass().getMethod("map", Function.class);
            return map.invoke(result, (Function<Object, Object>) v -> {
                translator.accept(v);
                return v;
            });
        } catch (Throwable t) {
            return null;
        }
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
