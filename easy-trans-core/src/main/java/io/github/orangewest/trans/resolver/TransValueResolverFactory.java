package io.github.orangewest.trans.resolver;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * {@link TransValueResolver} 的静态注册表（对齐 {@code TransRepositoryFactory} 模式）。
 *
 * <p>默认注册 JDK 层的 {@link CompletionStageResolver}；Project Reactor 解析器由 spring-start 在 reactor
 * 位于 classpath 时经 Spring {@code @ConditionalOnClass} 注入并注册，本工厂与 core 引擎均不静态引用 reactor，
 * 保持 GraalVM Native 下纯 MVC 应用不依赖 reactor。用户自定义 {@link TransValueResolver} 经
 * {@code TransValueResolverFactory.register(...)} 或 Spring Bean（{@code EasyTransRegister}）追加。
 */
public final class TransValueResolverFactory {

    private static final List<TransValueResolver> RESOLVERS = new CopyOnWriteArrayList<>();

    static {
        RESOLVERS.add(new CompletionStageResolver());
    }

    private TransValueResolverFactory() {
    }

    public static void register(TransValueResolver resolver) {
        RESOLVERS.add(resolver);
    }

    public static TransValueResolver firstSupports(Object value) {
        Class<?> type = value.getClass();
        for (TransValueResolver resolver : RESOLVERS) {
            if (resolver.supports(type)) {
                return resolver;
            }
        }
        return null;
    }
}
