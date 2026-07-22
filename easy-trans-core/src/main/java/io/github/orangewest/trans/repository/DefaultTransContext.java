package io.github.orangewest.trans.repository;

import java.util.Collections;
import java.util.Map;

/**
 * {@link TransContext} 的默认实现。属性值由框架在解析阶段从源注解反射提取后注入，
 * 运行时零反射。
 */
public class DefaultTransContext implements TransContext {

    private final String repoName;

    private final Map<String, Object> attributes;

    private final Class<?> sourceType;

    public DefaultTransContext(String repoName, Map<String, Object> attributes) {
        this(repoName, attributes, null);
    }

    public DefaultTransContext(String repoName, Map<String, Object> attributes, Class<?> sourceType) {
        this.repoName = repoName;
        this.attributes = attributes == null ? Collections.emptyMap() : attributes;
        this.sourceType = sourceType;
    }

    @Override
    public Object get(String attribute) {
        return attributes.get(attribute);
    }

    @Override
    public <V> V get(String attribute, Class<V> type) {
        return type.cast(attributes.get(attribute));
    }

    @Override
    public String repoName() {
        return repoName;
    }

    @Override
    public Class<?> sourceType() {
        return sourceType;
    }

}
