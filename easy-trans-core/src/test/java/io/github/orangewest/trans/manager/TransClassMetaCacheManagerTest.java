package io.github.orangewest.trans.manager;

import io.github.orangewest.trans.core.TransClassMeta;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TransClassMetaCacheManagerTest {

    @Test
    void same_class_returns_cached_instance() {
        TransClassMeta first = TransClassMetaCacheManager.getTransClassMeta(String.class);
        TransClassMeta second = TransClassMetaCacheManager.getTransClassMeta(String.class);
        assertSame(first, second, "metadata for the same class must be cached, not rebuilt");
    }

    @Test
    void distinct_classes_are_cached_separately() {
        TransClassMeta a = TransClassMetaCacheManager.getTransClassMeta(String.class);
        TransClassMeta b = TransClassMetaCacheManager.getTransClassMeta(Integer.class);
        assertNotSame(a, b);
    }

    @Test
    void lru_evicts_least_recently_used() throws Exception {
        // 直接构造一个容量为 3 的 LruCache 验证淘汰逻辑，避免需要 1024 个真实类
        Class<?> lruClass = Class.forName(
                "io.github.orangewest.trans.manager.TransClassMetaCacheManager$LruCache");
        Constructor<?> ctor = lruClass.getDeclaredConstructor(int.class);
        ctor.setAccessible(true);
        @SuppressWarnings("unchecked")
        LinkedHashMap<String, TransClassMeta> cache =
                (LinkedHashMap<String, TransClassMeta>) ctor.newInstance(3);

        cache.put("a", null);
        cache.put("b", null);
        cache.put("c", null);
        // 触发淘汰：第 4 个元素加入后应淘汰最久未访问的 "a"
        cache.put("d", null);
        cache.put("e", null);

        assertTrue(cache.size() <= 3, "LRU cache must not exceed its capacity, size=" + cache.size());
        assertNull(cache.get("a"), "least recently used entry 'a' should have been evicted");
        assertNull(cache.get("b"), "entry 'b' should have been evicted");
        assertTrue(cache.containsKey("c"), "recently used 'c' should remain");
        assertTrue(cache.containsKey("d"), "recently used 'd' should remain");
        assertTrue(cache.containsKey("e"), "recently used 'e' should remain");
    }
}
