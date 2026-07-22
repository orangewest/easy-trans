package io.github.orangewest.trans.manager;

import io.github.orangewest.trans.core.TransClassMeta;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

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
}
