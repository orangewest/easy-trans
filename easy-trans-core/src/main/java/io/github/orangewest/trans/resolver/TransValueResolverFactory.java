package io.github.orangewest.trans.resolver;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * {@link TransValueResolver} static registry (mirrors {@code TransRepositoryFactory} pattern).
 *
 * <p>Registers JDK-layer {@link CompletionStageResolver} by default; Project Reactor
 * resolvers are injected by spring-start when reactor is on the classpath via a Spring
 * {@code @ConditionalOnClass} bean. This factory and the core engine never statically
 * reference reactor, keeping GraalVM Native images free of reactor for plain MVC apps.
 *
 * <p>User-defined {@link TransValueResolver}s are added via
 * {@code TransValueResolverFactory.register(...)} or as Spring beans
 * ({@code EasyTransRegister}). Resolvers are ordered by {@link TransValueResolver#priority()}
 * (lower values checked first, matching {@code @Order} semantics); built-in resolvers
 * use {@code Integer.MAX_VALUE} so user resolvers preempt them by default (0 &lt; MAX_VALUE).
 */
public final class TransValueResolverFactory {

    private static final List<TransValueResolver> RESOLVERS = new CopyOnWriteArrayList<>();

    static {
        RESOLVERS.add(new CompletionStageResolver());
    }

    private TransValueResolverFactory() {
    }

    /**
     * Register a resolver with its own {@link TransValueResolver#priority()}.
     */
    public static void register(TransValueResolver resolver) {
        register(resolver, resolver.priority());
    }

    /**
     * Register a resolver with an explicit priority. Higher values are checked
     * earlier by {@link #firstSupports}. The resolver is inserted at the correct
     * position to maintain descending priority order.
     */
    public static void register(TransValueResolver resolver, int priority) {
        int insertAt = 0;
        for (TransValueResolver r : RESOLVERS) {
            if (r.priority() >= priority) {
                break;
            }
            insertAt++;
        }
        RESOLVERS.add(insertAt, resolver);
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
