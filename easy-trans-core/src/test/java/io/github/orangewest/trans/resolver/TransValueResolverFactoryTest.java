package io.github.orangewest.trans.resolver;

import org.junit.jupiter.api.Test;

import java.util.function.Function;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies that {@link TransValueResolverFactory} respects
 * {@link TransValueResolver#priority()} ordering (lower = earlier).
 */
class TransValueResolverFactoryTest {

    static class LowPriorityFutureResolver implements TransValueResolver {
        @Override
        public boolean supports(Class<?> type) {
            return CompletableFuture.class.isAssignableFrom(type);
        }

        @Override
        public Object handle(Object value, Function<Object, Object> translator) {
            return "intercepted";
        }

        @Override
        public int priority() {
            return -100; // lower than default 0 → checked earlier
        }
    }

    @Test
    void lowerPriorityPreemptsHigher() {
        LowPriorityFutureResolver lowPrio = new LowPriorityFutureResolver();
        TransValueResolverFactory.register(lowPrio);

        // lowPrio(-100) should be checked before built-in(MAX_VALUE)
        // and before resolvers with default priority 0.
        TransValueResolver resolved = TransValueResolverFactory.firstSupports(new CompletableFuture<>());
        assertSame(lowPrio, resolved,
                "lower priority resolver should preempt built-in");
        assertEquals("intercepted",
                resolved.handle(new CompletableFuture<>(), Function.identity()));
    }

    @Test
    void explicitPriorityOrdersCorrectly() {
        var first = new LowPriorityFutureResolver() {
            @Override
            public int priority() { return 50; }
            @Override
            public Object handle(Object v, Function<Object, Object> t) { return "50"; }
        };
        var second = new LowPriorityFutureResolver() {
            @Override
            public int priority() { return 10; }
            @Override
            public Object handle(Object v, Function<Object, Object> t) { return "10"; }
        };

        // Register in forward order: 50, then 10
        TransValueResolverFactory.register(first, 50);
        TransValueResolverFactory.register(second, 10);

        // Even though 50 was registered first, 10 has lower priority
        // and should be checked first (among these two).
        // Note: if a -100 resolver was already registered by another test,
        // it will match first -- that's still ascending priority order.
        TransValueResolver resolved = TransValueResolverFactory.firstSupports(new CompletableFuture<>());
        assertNotNull(resolved);
        assertTrue(resolved.priority() <= 10,
                "resolved resolver should have priority <= 10, got: " + resolved.priority());
    }
}
