package io.github.orangewest.trans.manager;


import io.github.orangewest.trans.core.TransClassMeta;

import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 类管理器
 */
public class TransClassMetaCacheManager implements Serializable {

    private static final long serialVersionUID = 3076627700677041940L;

    private static final Map<String, TransClassMeta> CACHE = new ConcurrentHashMap<>();

    public static TransClassMeta getTransClassMeta(Class<?> clazz) {
        TransClassMeta temp = CACHE.get(clazz.getName());
        if (null == temp) {
            temp = new TransClassMeta(clazz);
            if (temp.needTrans()) {
                CACHE.put(clazz.getName(), temp);
            }
        }
        return temp;
    }

}
