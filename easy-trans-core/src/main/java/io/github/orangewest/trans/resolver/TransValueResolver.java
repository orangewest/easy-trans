package io.github.orangewest.trans.resolver;

import java.util.function.Function;

/**
 * 翻译值适配器：对传入翻译的值做适配，决定「翻译什么、何时翻译」。
 *
 * <p>统一了原先两类扩展点：
 * <ul>
 *   <li>对象拆包（原 {@code TransObjResolver}）：把 {@code Result<T>}、{@code PageData<T>} 等包装解析为内部业务对象；</li>
 *   <li>返回值处理（原 {@code TransResultHandler}）：对 {@code CompletableFuture}、{@code Mono}/{@code Flux} 等
 *       异步/响应式返回值，延迟到结果就绪后再翻译。</li>
 * </ul>
 * 二者同属「对一个值做适配、返回（可能已翻译的）值」，故归一为单一接口。
 *
 * <p>匹配规则：{@link TransValueResolverFactory#firstSupports(Object)} 按值的类型取第一个 {@link #supports(Class)} 命中的解析器负责处理。
 * 同步/兜底翻译由 {@code TransService.trans} 在不命中任何解析器时执行，无需单独的 catch-all 解析器。
 */
public interface TransValueResolver {

    /**
     * 该适配器能否处理此类型的值（按类型匹配，不依赖具体实例）。
     */
    boolean supports(Class<?> type);

    /**
     * 处理该值：拆包后递归翻译、或延迟翻译后返回包装对象。
     *
     * @param value      待处理的值
     * @param translator 翻译动作（{@code TransService.trans}，对子值递归适配与翻译）
     * @return 处理后的对象（同步时为已翻译对象；异步时为包裹了翻译逻辑的对象）
     */
    Object handle(Object value, Function<Object, Object> translator);

    /**
     * Priority for resolver ordering. Lower values are checked earlier (matching
     * {@code @Order} / {@code jakarta.annotation.Priority} semantics). Default 0.
     * Built-in resolvers use {@link Integer#MAX_VALUE} so user resolvers preempt
     * them without explicit priority configuration (0 &lt; MAX_VALUE).
     */
    default int priority() {
        return 0;
    }
}
