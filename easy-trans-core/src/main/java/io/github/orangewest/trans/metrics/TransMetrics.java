package io.github.orangewest.trans.metrics;

/**
 * 翻译监控指标接口。
 * <p>
 * 框架核心层不依赖任何监控库，仅定义抽象接口；具体实现（如 Micrometer 桥接）放在 spring-start 中。
 * 引擎在翻译时调用本接口记录指标，未注册实现时退化为 {@link NoopTransMetrics}（无任何开销）。
 */
public interface TransMetrics {

    /**
     * 记录一次 {@code trans()} 翻译调用的耗时与结果。
     *
     * @param durationNanos 耗时（纳秒）
     * @param success       是否成功（抛异常为 false）
     */
    void recordTranslate(long durationNanos, boolean success);

    /**
     * 记录单个翻译仓库的耗时与结果。
     *
     * @param repoName      仓库名称（{@code @TransRepo} 字段名或 {@code @Trans} 源字段名）
     * @param durationNanos 耗时（纳秒）
     * @param success       是否成功
     */
    void recordRepository(String repoName, long durationNanos, boolean success);

}
