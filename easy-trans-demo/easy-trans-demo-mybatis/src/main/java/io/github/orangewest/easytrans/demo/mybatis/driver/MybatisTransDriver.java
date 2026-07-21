package io.github.orangewest.easytrans.demo.mybatis.driver;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.toolkit.SqlHelper;
import io.github.orangewest.easytrans.demo.mybatis.entity.BaseEntity;
import org.apache.ibatis.session.SqlSession;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.util.List;

/**
 * MyBatis-Plus 实现的 {@link TransDriver}：借助 MyBatis-Plus 的 {@link SqlHelper}，
 * 按实体类直接获取对应的 {@link BaseMapper} 与 {@link SqlSession}，
 * 无需手工维护「实体类 → Mapper」的注册表。
 *
 * <p>新增实体时，只需提供继承 {@code BaseMapper<T>} 的 Mapper（如 {@code TeacherMapper}），
 * 本驱动即可自动按实体类定位它；旧的 {@code MAPPER_REGISTRY} 手工登记方式容易漏登、出错。
 */
@Component
public class MybatisTransDriver implements TransDriver {

    @Override
    @SuppressWarnings("unchecked")
    public List<? extends BaseEntity> findByIds(List<? extends Serializable> ids, Class<? extends BaseEntity> targetClass) {
        try (SqlSession sqlSession = SqlHelper.sqlSession(targetClass)) {
            BaseMapper<? extends BaseEntity> mapper = SqlHelper.getMapper(targetClass, sqlSession);
            return mapper.selectBatchIds(ids);
        }
    }
}
