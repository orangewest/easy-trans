package io.github.orangewest.trans.resolver;

/**
 * 翻译包装对象解析器
 */
public interface TransObjResolver {

    boolean support(Object obj);

    /**
     * 解析包装对象，获取需要翻译的对象
     *
     * @param obj 原包装对象
     * @return 需要翻译的对象
     */
    Object resolveTransObj(Object obj);

}
