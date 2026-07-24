package io.github.orangewest.trans.propagation;

import java.util.List;

/**
 * 组合多个 {@link TransContextPropagator}：把它们串成一个，供翻译引擎并行分支统一调用。
 * <p>
 * 快照按 delegate 下标对齐存放于 {@code Object[]}，各 delegate 的快照互相独立、互不覆盖
 * （如 SecurityContext 与 MDC 各占一格）。执行顺序：{@link #capture()} / {@link #restore(Object)}
 * 按注册顺序，{@link #clear()} 逆序（对称、稳妥）。
 * <p>
 * 包级私有：仅由 {@link TransContextPropagatorFactory} 在注册多个传播器时构造；
 * 0 个用 {@link TransContextPropagator#NOOP}、1 个直接使用其本身，均不经过本类。
 */
final class CompositeContextPropagator implements TransContextPropagator {

    private final TransContextPropagator[] delegates;

    CompositeContextPropagator(List<TransContextPropagator> delegates) {
        this.delegates = delegates.toArray(new TransContextPropagator[0]);
    }

    @Override
    public Object capture() {
        Object[] snapshots = new Object[delegates.length];
        for (int i = 0; i < delegates.length; i++) {
            snapshots[i] = delegates[i].capture();
        }
        return snapshots;
    }

    @Override
    public void restore(Object snapshot) {
        Object[] snapshots = (Object[]) snapshot;
        for (int i = 0; i < delegates.length; i++) {
            delegates[i].restore(snapshots[i]);
        }
    }

    @Override
    public void clear() {
        // 逆序清理，与 restore 顺序对称
        for (int i = delegates.length - 1; i >= 0; i--) {
            delegates[i].clear();
        }
    }
}
