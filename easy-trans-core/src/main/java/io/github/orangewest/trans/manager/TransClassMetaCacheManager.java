package io.github.orangewest.trans.manager;


import io.github.orangewest.trans.core.TransClassMeta;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 类管理器：缓存每个被翻译类的元数据 {@link TransClassMeta}。
 * <p>
 * 早期实现使用无界的 {@link java.util.concurrent.ConcurrentHashMap}，长时间运行且翻译类持续增多的场景下会导致元数据无限堆积。
 * 这里改为有界 LRU 缓存：超出容量时淘汰最久未使用的条目，保证内存占用有上限。
 */
public class TransClassMetaCacheManager implements Serializable {

    private static final long serialVersionUID = 3076627700677041940L;

    /**
     * 缓存容量上限。对于常规应用，被翻译的 DTO 类数量远小于此值，超出后才触发 LRU 淘汰。
     */
    private static final int MAX_CACHE_SIZE = 1024;

    private static final Map<String, TransClassMeta> CACHE = new LruCache(MAX_CACHE_SIZE);

    public static TransClassMeta getTransClassMeta(Class<?> clazz) {
        String key = clazz.getName();
        synchronized (CACHE) {
            TransClassMeta meta = CACHE.get(key);
            if (meta == null) {
                meta = new TransClassMeta(clazz);
                CACHE.put(key, meta);
            }
            return meta;
        }
    }

    /**
     * 基于 {@link LinkedHashMap}（accessOrder=true）的 LRU 缓存，超出容量时自动淘汰最久未访问的条目。
     */
    private static final class LruCache extends LinkedHashMap<String, TransClassMeta> {

        private static final long serialVersionUID = 1L;

        private final int maxSize;

        LruCache(int maxSize) {
            super(16, 0.75f, true);
            this.maxSize = maxSize;
        }

        @Override
        protected boolean removeEldestEntry(Map.Entry<String, TransClassMeta> eldest) {
            return size() > maxSize;
        }
    }

}
