package io.github.orangewest.trans.repository;

import io.github.orangewest.trans.core.TransModel;

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
     * @param transModels 翻译对象
     * @return 查询结果值
     */
    default Map<Object, Object> getTransValueMap(List<TransModel> transModels, List<Object> transValues) {
        return Collections.emptyMap();
    }

    /**
     * 获取翻译结果（适用于字典）
     *
     * @param keys        需要翻译的值
     * @param transModels 翻译对象
     * @return 查询结果值
     */
    default Map<Object, Object> getKeysMap(List<TransModel> transModels, List<String> keys) {
        return Collections.emptyMap();
    }

}
