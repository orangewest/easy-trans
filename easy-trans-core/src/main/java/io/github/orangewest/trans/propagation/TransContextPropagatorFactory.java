package io.github.orangewest.trans.propagation;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * {@link TransContextPropagator} 静态注册器（沿用 {@code TransRepositoryFactory} /
 * {@code TransValueResolverFactory} / {@code TransMetricsCollector} 的静态注册模式）。
 * <p>
 * 引擎通过 {@link #get()} 获取当前组合后的传播器；集成层（spring-start）在启动时对每个
 * {@code TransContextPropagator} Bean 调用 {@link #register(TransContextPropagator)}（由
 * {@code EasyTransRegister} 完成），非 Spring 环境用户直接调用 {@link #register(TransContextPropagator)}。
 * <p>
 * 注册多个时按注册顺序组合为 {@code CompositeContextPropagator}；0 个时 {@link #get()} 返回
 * {@link TransContextPropagator#NOOP}（未引入也不退化），1 个时直接返回其本身。
 */
public final class TransContextPropagatorFactory {

    private static final List<TransContextPropagator> PROPAGATORS = new CopyOnWriteArrayList<>();

    private static volatile TransContextPropagator combined = TransContextPropagator.NOOP;

    private TransContextPropagatorFactory() {
    }

    public static void register(TransContextPropagator propagator) {
        if (propagator == null) {
            return;
        }
        PROPAGATORS.add(propagator);
        rebuild();
    }

    /**
     * @return 当前组合后的传播器；无任何注册时为 {@link TransContextPropagator#NOOP}。
     */
    public static TransContextPropagator get() {
        return combined;
    }

    /**
     * 清空所有已注册传播器（主要供测试隔离使用）。
     */
    public static void clear() {
        PROPAGATORS.clear();
        rebuild();
    }

    private static void rebuild() {
        List<TransContextPropagator> snapshot = List.copyOf(PROPAGATORS);
        if (snapshot.isEmpty()) {
            combined = TransContextPropagator.NOOP;
        } else if (snapshot.size() == 1) {
            combined = snapshot.getFirst();
        } else {
            combined = new CompositeContextPropagator(snapshot);
        }
    }
}
