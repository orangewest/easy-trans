package io.github.orangewest.trans.spring.propagation;

import io.github.orangewest.trans.propagation.TransContextPropagator;
import io.micrometer.context.ContextSnapshot;
import io.micrometer.context.ContextSnapshotFactory;

/**
 * 基于 Micrometer {@code context-propagation} 的 {@link TransContextPropagator} 桥接实现。
 * <p>
 * 仅当 classpath 存在 {@code io.micrometer.context.ContextSnapshotFactory} 时由
 * {@code EasyTransAutoConfiguration} 自动装配（{@code context-propagation} 在 spring-start 中为 optional 依赖）。
 * 未引入时不装配，框架退回 {@link TransContextPropagator#NOOP} 或用户自定义实现。
 * <p>
 * 它一把抓取<b>所有已注册 {@code ThreadLocalAccessor}</b> 的上下文（Spring Security、SLF4J MDC、
 * Reactor 等，谁注册了 accessor 就带谁），无需针对每个上下文单独写 Propagator。
 * <p>
 * <b>Scope 生命周期</b>：Micrometer 的 {@link ContextSnapshot#setThreadLocals()} 会先保存当前值、
 * 写入快照，并返回一个需在还原时 {@code close()} 的 {@link ContextSnapshot.Scope}。由于 {@code restore}
 * 与 {@code clear} 保证在<b>同一虚拟线程</b>上成对调用，这里用一个 {@link ThreadLocal} 承接该 Scope，
 * {@code clear} 时关闭并移除——{@link TransContextPropagator} 接口本身无需为此扩展签名。
 */
public class MicrometerContextPropagator implements TransContextPropagator {

    private final ContextSnapshotFactory factory = ContextSnapshotFactory.builder().build();

    private final ThreadLocal<ContextSnapshot.Scope> scopeHolder = new ThreadLocal<>();

    @Override
    public Object capture() {
        return factory.captureAll();
    }

    @Override
    public void restore(Object snapshot) {
        scopeHolder.set(((ContextSnapshot) snapshot).setThreadLocals());
    }

    @Override
    public void clear() {
        ContextSnapshot.Scope scope = scopeHolder.get();
        if (scope != null) {
            scope.close();
            scopeHolder.remove();
        }
    }
}
