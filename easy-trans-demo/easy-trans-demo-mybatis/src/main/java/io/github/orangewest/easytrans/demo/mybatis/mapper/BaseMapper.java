package io.github.orangewest.easytrans.demo.mybatis.mapper;

import io.github.orangewest.easytrans.demo.mybatis.entity.BaseEntity;
import org.apache.ibatis.annotations.Param;

import java.io.Serializable;
import java.util.List;

/**
 * 通用 Mapper（对标 MyBatis-Plus 的 {@code BaseMapper}）：所有实体 Mapper 继承它即可获得按 ids 批量查询能力。
 */
public interface BaseMapper<T extends BaseEntity> {

    List<T> selectBatchIds(@Param("ids") List<Serializable> ids);
}
