package io.github.orangewest.trans.util;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class CollectionUtils {

    public static boolean isEmpty(Collection<?> collection) {
        return (collection == null || collection.isEmpty());
    }

    public static boolean isNotEmpty(Collection<?> collection) {
        return !isEmpty(collection);
    }

    public static boolean isEmpty(Map<?, ?> map) {
        return (map == null || map.isEmpty());
    }

    public static boolean isNotEmpty(Map<?, ?> map) {
        return !isEmpty(map);
    }

    public static List<Object> objToList(Object obj) {
        List<Object> objList;
        if (obj instanceof Iterable<?>) {
            Iterable<?> iterable = (Iterable<?>) obj;
            objList = StreamSupport.stream(iterable.spliterator(), false).collect(Collectors.toList());
        } else if (obj.getClass().isArray()) {
            objList = Arrays.stream((Object[]) obj).collect(Collectors.toList());
        } else {
            objList = Collections.singletonList(obj);
        }
        return objList;
    }

}
