package io.github.orangewest.trans.manager;


import io.github.orangewest.trans.core.TransClassMeta;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 类管理器：缓存每个被翻译类的元数据 {@link TransClassMeta}。
 * <p>
 * 使用 {@link ConcurrentHashMap} 提供无锁的并发读取：{@code computeIfAbsent} 保证每个类只构建一次，
 * 且读取路径完全无锁。早期实现为带 accessOrder 的有界 LRU（{@link java.util.LinkedHashMap}），
 * 但因 accessOrder 下每次 get 都会重排链表，读路径必须加锁，无法并发。
 * <p>
 * 被翻译的 DTO 类数量在常规应用中是有界的（等于应用中带 {@code @Trans} 的类数），且一旦构建基本常驻，
 * 故不再做有界 LRU 淘汰——无界 {@link ConcurrentHashMap} 在此场景下内存占用同样有界。
 */
public class TransClassMetaCacheManager {

    private static final Map<String, TransClassMeta> CACHE = new ConcurrentHashMap<>();

    public static TransClassMeta getTransClassMeta(Class<?> clazz) {
        return CACHE.computeIfAbsent(clazz.getName(), _ -> new TransClassMeta(clazz));
    }

}
