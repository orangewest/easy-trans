package io.github.orangewest.trans.repository;

/**
 * 传递给 {@link TransRepository#getTransValueMap(List, TransContext)} 的翻译上下文。
 *
 * <p>框架在<b>解析阶段</b>（每个被翻译类首次遇到时，仅一次）通过反射读取源注解
 * （{@code @TransRepo} / 自定义元注解 / {@code @Trans(using=...)}）的属性，集中放入本上下文。
 * 运行时只读取上下文，不再做任何反射，因此仓库实现可以在 GraalVM Native Image 下保持 AOT 干净。
 *
 * <p>仓库通过 {@link #get(String, Class)} 按属性名取用，例如字典翻译取 {@code group}：
 * <pre>{@code
 * String group = context.get("group", String.class);
 * }</pre>
 */
public interface TransContext {

    /**
     * 按属性名获取翻译注解上的属性值。
     *
     * @param attribute 属性名（如 {@code "group"}、{@code "name"}、{@code "trans"}）
     * @return 属性值；该属性不存在时返回 {@code null}
     */
    Object get(String attribute);

    /**
     * 按属性名获取翻译注解上的属性值，并按预期类型强转。
     *
     * @param attribute 属性名
     * @param type      预期值类型
     * @param <V>       预期值类型
     * @return 强转后的属性值；属性不存在或为 {@code null} 时返回 {@code null}
     * @throws ClassCastException 属性值无法转换为 {@code type} 时
     */
    <V> V get(String attribute, Class<V> type);

    /**
     * @return 当前翻译仓库名（{@code @TransRepo} 的 name、字段名或 {@code @Trans(using=...)} 的命名结果）
     */
    String repoName();

    /**
     * 源字段的类型（被 {@code @TransRepo} / {@code @EnumTrans} 标注的字段）。
     * 默认实现返回 {@code null}；框架默认实现会填入真实类型，供仓库（如枚举即字典）推断枚举类。
     *
     * @return 源字段类型；无法获取时为 {@code null}
     */
    default Class<?> sourceType() {
        return null;
    }

}
