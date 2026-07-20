package io.github.orangewest.easytrans.demo.mybatis.driver;

import io.github.orangewest.easytrans.demo.mybatis.entity.BaseEntity;

import java.io.Serializable;
import java.util.List;

/**
 * 数据库驱动抽象：屏蔽 MyBatis / JPA 差异，按 ids 批量加载实体。
 * 在 MyBatis / JPA 两套 demo 中各提供一个实现。
 */
public interface TransDriver {

    /**
     * 根据 ids 批量加载实体。
     *
     * @param ids         id 列表
     * @param targetClass 目标实体类
     * @return 命中的实体列表（可能少于 ids，未命中者不在结果中）
     */
    List<? extends BaseEntity> findByIds(List<? extends Serializable> ids, Class<? extends BaseEntity> targetClass);
}
