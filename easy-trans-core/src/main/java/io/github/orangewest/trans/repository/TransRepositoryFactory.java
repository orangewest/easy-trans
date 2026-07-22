package io.github.orangewest.trans.repository;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TransRepositoryFactory {

    /**
     * key 为仓库 Class，val 为对应类型的 TransRepository 实例。
     * 这是一个异构容器：Map 无法在静态类型上约束「key 的泛型 == value 的泛型」，
     * 故 value 侧只能以 {@code TransRepository<?, ?>} 存放；key 用擦除后的 {@code Class<?>}
     * （{@code getClass()} 返回的原始边界），对外暴露的 {@link #getTransRepository} 仍以
     * {@code Class<? extends TransRepository<?, ?>>} 约束入参。
     */
    private final static Map<Class<?>, TransRepository<?, ?>> TRANS_REPOSITORY_MAP = new ConcurrentHashMap<>();

    /**
     * 取出已注册的仓库。由于存在类型擦除，从 {@code TransRepository<?, ?>} 取回的实例
     * 只能以 {@code TransRepository<Object, Object>} 形式交付给翻译引擎使用，该转型不受检——
     * 这是「类型安全的异构容器」模式（Effective Java Item 33）固有且唯一需要抑制的一处。
     */
    @SuppressWarnings("unchecked")
    public static TransRepository<Object, Object> getTransRepository(Class<? extends TransRepository<?, ?>> repository) {
        return (TransRepository<Object, Object>) TRANS_REPOSITORY_MAP.get(repository);
    }

    public static void register(TransRepository<?, ?> transRepository) {
        if (transRepository == null) {
            return;
        }
        TRANS_REPOSITORY_MAP.put(transRepository.getClass(), transRepository);
    }

}
