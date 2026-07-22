package io.github.orangewest.trans.core;

import io.github.orangewest.trans.repository.TransRepository;
import io.github.orangewest.trans.util.ReflectUtils;
import io.github.orangewest.trans.util.StringUtils;

import java.lang.reflect.Field;
import java.util.Map;

public class TransRepoMeta {

    /**
     * 仓库名称
     */
    private final String repoName;

    /**
     * 仓库对应的属性
     */
    private final Field repoField;

    /**
     * 源注解（{@code @TransRepo} / 自定义元注解 / {@code @Trans(using=...)}) 的属性，
     * 由框架在解析阶段通过反射提取并缓存，运行时零反射。
     */
    private final Map<String, Object> attributes;

    /**
     * 属性使用的翻译仓库
     */
    private final Class<? extends TransRepository<?, ?>> repository;


    public TransRepoMeta(String repoName, Field repoField, Map<String, Object> attributes, Class<? extends TransRepository<?, ?>> repository) {
        // 与 TransClassMeta.generateRepoName 保持一致：显式 name 优先，否则回退到源字段名（map key 也用此值，
        // 保证 buildTransTree 的树根检测、指标 repoName、TransContext 三者口径统一）。
        this.repoName = StringUtils.isNotEmpty(repoName) ? repoName
                : (repoField != null ? repoField.getName() : repoName);
        this.repoField = repoField;
        this.attributes = attributes;
        this.repository = repository;
        // 解析期一次性 setAccessible（ADR-0003）：运行期读取 repoField 不再重复检查
        ReflectUtils.setAccessible(repoField);
    }

    public String getRepoName() {
        return repoName;
    }

    public Field getRepoField() {
        return repoField;
    }

    public Map<String, Object> getAttributes() {
        return attributes;
    }

    public Class<? extends TransRepository<?, ?>> getRepository() {
        return repository;
    }

}
