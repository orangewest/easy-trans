package io.github.orangewest.trans.repository;

import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 获取已翻译数据仓库
 */
public interface TransRepository {

    /**
     * 获取翻译结果（适用于数据库等翻译）
     *
     * @param transValues 需要翻译的值
     * @param transAnno   翻译对象上的注解
     * @return 查询结果值 val-翻译值
     */
    default Map<Object, Object> getTransValueMap(List<Object> transValues, Annotation transAnno) {
        return Collections.emptyMap();
    }

}
