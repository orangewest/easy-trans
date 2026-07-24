package io.github.orangewest.trans.metrics;

import java.lang.annotation.Annotation;

/**
 * 翻译测量的语义上下文对象（不可变）。
 * <p>
 * 引擎在开启一个 Span 时构造本对象，携带本次测量点的结构化语义信息；后端按
 * <b>low / high cardinality</b> 自行映射到 tag / attribute，从根本上规避高基数爆炸：
 * <ul>
 *     <li>low cardinality（默认进 tag）：{@code operation}、{@code depth}、{@code parent}（父子链路）、
 *     {@code repoName}（经 Micrometer 映射为 {@code repo}）</li>
 *     <li>high cardinality（默认不进 tag，需经 {@code setAttribute} 或配置开启）：
 *     {@code targetClass}、{@code repositoryClass}、{@code annotation}</li>
 * </ul>
 * 实例通过 {@link #builder(String)} 链式构造，构造完成后不可修改（builder 与实例分离）。
 */
public final class TransMetricContext {

    private final String operation;
    private final int depth;
    private final TransMetrics.Span parent;
    private final Class<?> targetClass;
    private final String repoName;
    private final Class<?> repositoryClass;
    private final Annotation annotation;

    private TransMetricContext(Builder b) {
        this.operation = b.operation;
        this.depth = b.depth;
        this.parent = b.parent;
        this.targetClass = b.targetClass;
        this.repoName = b.repoName;
        this.repositoryClass = b.repositoryClass;
        this.annotation = b.annotation;
    }

    public static Builder builder(String operation) {
        return new Builder(operation);
    }

    public String getOperation() {
        return operation;
    }

    public int getDepth() {
        return depth;
    }

    public TransMetrics.Span getParent() {
        return parent;
    }

    public Class<?> getTargetClass() {
        return targetClass;
    }

    public String getRepoName() {
        return repoName;
    }

    public Class<?> getRepositoryClass() {
        return repositoryClass;
    }

    public Annotation getAnnotation() {
        return annotation;
    }

    /**
     * 链式 builder，所有 setter 返回自身以便链式调用；{@link #build()} 后实例不可变。
     */
    public static final class Builder {

        private final String operation;
        private int depth;
        private TransMetrics.Span parent;
        private Class<?> targetClass;
        private String repoName;
        private Class<?> repositoryClass;
        private Annotation annotation;

        private Builder(String operation) {
            this.operation = operation;
        }

        public Builder parent(TransMetrics.Span parent) {
            this.parent = parent;
            return this;
        }

        public Builder depth(int depth) {
            this.depth = depth;
            return this;
        }

        public Builder targetClass(Class<?> targetClass) {
            this.targetClass = targetClass;
            return this;
        }

        public Builder repoName(String repoName) {
            this.repoName = repoName;
            return this;
        }

        public Builder repositoryClass(Class<?> repositoryClass) {
            this.repositoryClass = repositoryClass;
            return this;
        }

        public Builder annotation(Annotation annotation) {
            this.annotation = annotation;
            return this;
        }

        public TransMetricContext build() {
            return new TransMetricContext(this);
        }
    }
}
