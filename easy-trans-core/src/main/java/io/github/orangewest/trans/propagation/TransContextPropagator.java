package io.github.orangewest.trans.propagation;

/**
 * 上下文传播 SPI：把<b>调用线程</b>的上下文（如 Spring Security {@code SecurityContextHolder}、
 * SLF4J {@code MDC}、JPA Session、租户 ID 等基于 {@link ThreadLocal} 的上下文）传播到并行取数的
 * 虚拟线程上。
 * <p>
 * 背景：翻译引擎在 DTO 含 2+ 仓库分组、或单仓库大列表分片时，会把查询派发到虚拟线程上并行执行。
 * 虚拟线程默认<b>不继承</b>调用线程的 {@code ThreadLocal}，因此仓库 {@code getTransValueMap} 里
 * 读不到 Session / 登录态 / trace-id。此前只能关闭并行来规避，本 SPI 让「并行」与「上下文正确」得以兼得。
 * <p>
 * 注册方式与 {@code TransRepository} / {@code TransValueResolver} 一致——通过静态工厂
 * {@link TransContextPropagatorFactory#register(TransContextPropagator)}；Spring 环境下标注
 * {@code @Component} 即由 {@code EasyTransRegister} 自动注册。可注册多个，各传播器互不耦合，
 * 由工厂组合（见 {@code CompositeContextPropagator}）。
 * <p>
 * 三档能力，且<b>不引入任何第三方依赖也不会退化</b>：
 * <ul>
 *   <li>未注册任何传播器 → 使用 {@link #NOOP}，行为与历史版本完全一致；</li>
 *   <li>自定义实现（仅依赖被传播库本身，如 spring-security-core）→ 精确控制某个上下文；</li>
 *   <li>Spring 环境存在 Micrometer {@code context-propagation} 时 → 自动桥接，一把带走所有已注册
 *       {@code ThreadLocalAccessor} 的上下文（桥接实现位于 easy-trans-spring-start）。</li>
 * </ul>
 * <p>
 * <b>调用时机契约</b>：{@link #capture()} 在调用线程、派发并行任务<b>之前</b>调用一次；
 * 得到的快照被传入每个并行任务，任务开始时调用 {@link #restore(Object)}，任务结束（{@code finally}）
 * 时调用 {@link #clear()}。{@code restore} 与 {@code clear} 保证在<b>同一个</b>虚拟线程上先后成对调用。
 */
public interface TransContextPropagator {

    /**
     * 在调用线程上抓取当前上下文快照。返回值将原样传给同一批并行任务的 {@link #restore(Object)}。
     *
     * @return 上下文快照（可为 {@code null}）
     */
    Object capture();

    /**
     * 在并行虚拟线程任务内、执行仓库查询<b>之前</b>调用，恢复 {@link #capture()} 得到的快照。
     *
     * @param snapshot {@link #capture()} 的返回值
     */
    void restore(Object snapshot);

    /**
     * 在并行任务结束后（{@code finally}）调用，清理本虚拟线程上被写入的上下文，避免线程复用时的污染。
     */
    void clear();

    /**
     * 空实现：未注册任何传播器时使用。{@code capture/restore/clear} 全部空转，零开销、零依赖，
     * 保证「不引入任何东西也不会退化」。
     */
    TransContextPropagator NOOP = new TransContextPropagator() {
        @Override
        public Object capture() {
            return null;
        }

        @Override
        public void restore(Object snapshot) {
        }

        @Override
        public void clear() {
        }
    };
}
