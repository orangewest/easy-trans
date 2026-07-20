package io.github.orangewest.easytrans.demo.mybatis.mapper;

import io.github.orangewest.easytrans.demo.mybatis.entity.Teacher;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.SelectProvider;

import java.io.Serializable;
import java.util.List;

/**
 * Teacher 的 Mapper：继承通用 {@link BaseMapper}，并声明具体的批量查询 SQL。
 */
@Mapper
public interface TeacherMapper extends BaseMapper<Teacher> {

    @Override
    @SelectProvider(type = TeacherSqlProvider.class, method = "selectBatchIds")
    List<Teacher> selectBatchIds(@Param("ids") List<Serializable> ids);
}
