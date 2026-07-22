package io.github.orangewest.easytrans.demo.mybatis.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.orangewest.easytrans.demo.mybatis.entity.Teacher;
import org.apache.ibatis.annotations.Mapper;

/**
 * Teacher 的 Mapper：直接继承 MyBatis-Plus 的 {@link BaseMapper} 即可获得 selectBatchIds 等通用方法，
 * 无需任何手写 SQL / SqlProvider。
 */
@Mapper
public interface TeacherMapper extends BaseMapper<Teacher> {
}
