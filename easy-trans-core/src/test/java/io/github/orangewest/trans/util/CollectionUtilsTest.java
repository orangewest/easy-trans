package io.github.orangewest.trans.util;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CollectionUtilsTest {

    @Test
    void objToListLongArray() {
        long[] arr = {1L, 2L, 3L};
        List<Object> result = CollectionUtils.objToList(arr);
        assertEquals(3, result.size());
        assertEquals(1L, result.get(0));
        assertEquals(2L, result.get(1));
        assertEquals(3L, result.get(2));
    }

    @Test
    void objToListIntArray() {
        int[] arr = {10, 20, 30};
        List<Object> result = CollectionUtils.objToList(arr);
        assertEquals(3, result.size());
        assertEquals(10, result.get(0));
        assertEquals(20, result.get(1));
        assertEquals(30, result.get(2));
    }

    @Test
    void objToListObjectArray() {
        String[] arr = {"a", "b"};
        List<Object> result = CollectionUtils.objToList(arr);
        assertEquals(2, result.size());
        assertEquals("a", result.get(0));
        assertEquals("b", result.get(1));
    }

    @Test
    void objToListEmptyArray() {
        int[] arr = {};
        List<Object> result = CollectionUtils.objToList(arr);
        assertEquals(0, result.size());
    }

}
