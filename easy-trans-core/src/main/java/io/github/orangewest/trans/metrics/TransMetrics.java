package io.github.orangewest.trans.metrics;

/**
 * 翻译监控指标接口（通用测量总线）。
 * <p>
 * 框架核心层不依赖任何监控库，仅定义抽象接口；具体实现（如 Micrometer Observation 桥接）放在 spring-start 中。
 * 引擎只负责「按 operation 发出测量点 + 携带语义上下文（{@link TransMetricContext}）」，具体如何呈现
 * （Timer / Counter / Tracing Span）完全交给后端，新增埋点无需改动本接口。
 * 未注册实现时退化为 {@link NoopTransMetrics}（零开销）。
 */
public interface TransMetrics {

    /**
     * 开启一段计时（Span）。context 携带语义上下文，可含父 Span 形成链路。
     *
     * @param operation 测量点类型，见 {@link TransMetricsOperations}
     * @param context   语义上下文（operation / depth / parent / targetClass / fieldName / repoName / repositoryClass / annotation）
     * @return Span 句柄，调用方负责在 finally 中调用 {@link Span#end()}
     */
    Span startSpan(String operation, TransMetricContext context);

    /**
     * 计数（设计预留，本期引擎暂不接任何埋点）。
     *
     * @param operation 测量点类型，见 {@link TransMetricsOperations}
     * @param context   语义上下文
     * @param n         计数值
     */
    void increment(String operation, TransMetricContext context, long n);

    /**
     * 一次测量点的句柄。实现可在 start 与 end 之间记录耗时、错误、附加属性等信息。
     */
    interface Span {

        /**
         * 可选：补充额外属性，后端按自身策略决定是否采用、以及是否作为 low / high cardinality 处理。
         */
        void setAttribute(String key, String value);

        /**
         * 标记本次调用发生异常（用于 success=false 等判定）。可在 end 之前调用。
         */
        void recordException(Throwable t);

        /**
         * 结束计时并提交指标。
         */
        void end();
    }

}
