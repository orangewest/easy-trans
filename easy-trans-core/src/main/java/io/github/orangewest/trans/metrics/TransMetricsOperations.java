package io.github.orangewest.trans.metrics;

/**
 * 翻译测量的 operation 常量。
 * <p>
 * 新增埋点只需新增一个常量，无需改动 {@link TransMetrics} 接口或引擎——引擎统一经
 * {@link TransMetrics#startSpan(String, TransMetricContext)} 发出测量点。
 */
public final class TransMetricsOperations {

    /** 整次 {@code trans()} 翻译调用。 */
    public static final String TRANSLATE = "translate";

    /** 单个翻译仓库查询。 */
    public static final String REPOSITORY = "repository";

    /** 单个目标字段的读取 + 写入。 */
    public static final String FIELD = "field";

    private TransMetricsOperations() {
    }
}
