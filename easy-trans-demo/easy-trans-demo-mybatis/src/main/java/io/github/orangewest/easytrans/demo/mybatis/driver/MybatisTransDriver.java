package io.github.orangewest.easytrans.demo.mybatis.driver;

import io.github.orangewest.easytrans.demo.mybatis.entity.BaseEntity;
import io.github.orangewest.easytrans.demo.mybatis.entity.Teacher;
import io.github.orangewest.easytrans.demo.mybatis.mapper.BaseMapper;
import io.github.orangewest.easytrans.demo.mybatis.mapper.TeacherMapper;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * MyBatis 实现的 {@link TransDriver}：通过注册表找到实体对应的 {@link BaseMapper}，按 ids 查库。
 *
 * <p>新增实体时，只需让其 Mapper 继承 {@code BaseMapper<T>} 并在此注册表登记即可。
 */
@Component
public class MybatisTransDriver implements TransDriver {

    @Autowired
    private SqlSessionTemplate sqlSessionTemplate;

    /** 实体类 -> 对应 Mapper 接口（泛型 BaseMapper 的映射注册表） */
    private final Map<Class<? extends BaseEntity>, Class<? extends BaseMapper<?>>> MAPPER_REGISTRY = new HashMap<>();

    public MybatisTransDriver() {
        MAPPER_REGISTRY.put(Teacher.class, TeacherMapper.class);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<? extends BaseEntity> findByIds(List<? extends Serializable> ids, Class<? extends BaseEntity> targetClass) {
        Class<? extends BaseMapper<?>> mapperClass = MAPPER_REGISTRY.get(targetClass);
        if (mapperClass == null) {
            throw new IllegalStateException("未注册实体 " + targetClass.getName() + " 对应的 Mapper");
        }
        BaseMapper<?> mapper = sqlSessionTemplate.getMapper(mapperClass);
        return mapper.selectBatchIds((List<Serializable>) ids);
    }
}
