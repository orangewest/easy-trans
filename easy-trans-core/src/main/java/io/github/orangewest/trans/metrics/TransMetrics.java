package io.github.orangewest.trans.metrics;

/**
 * 翻译监控指标接口。
 * <p>
 * 框架核心层不依赖任何监控库，仅定义抽象接口；具体实现（如 Micrometer Observation 桥接）放在 spring-start 中。
 * 引擎在翻译前调用 {@link #startTranslate()} / {@link #startRepository(String)} 获取一个 {@link Sample}，
 * 翻译结束后调用 {@link Sample#stop()}，异常时调用 {@link Sample#error(Throwable)}。
 * 未注册实现时退化为 {@link NoopTransMetrics}（零开销）。
 */
public interface TransMetrics {

    /**
     * 一次翻译 / 仓库调用的计时样本。实现可在 start 与 stop 之间记录耗时、错误等信息。
     */
    interface Sample {

        /**
         * 标记本次调用发生异常（用于 success=false 等判定）。可在 stop 之前调用。
         */
        void error(Throwable t);

        /**
         * 结束计时并提交指标。
         */
        void stop();
    }

    /**
     * 开始一次 {@code trans()} 翻译调用的计时。
     *
     * @return 计时样本，调用方负责在 finally 中调用 {@link Sample#stop()}
     */
    Sample startTranslate();

    /**
     * 开始单个翻译仓库调用的计时。
     *
     * @param repoName 仓库名称（{@code @TransRepo} 字段名或 {@code @Trans} 源字段名）
     * @return 计时样本，调用方负责在 finally 中调用 {@link Sample#stop()}
     */
    Sample startRepository(String repoName);

}
