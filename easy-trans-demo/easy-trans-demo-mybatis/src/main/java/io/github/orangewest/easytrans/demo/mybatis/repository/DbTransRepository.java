package io.github.orangewest.easytrans.demo.mybatis.repository;

import io.github.orangewest.easytrans.demo.mybatis.driver.TransDriver;
import io.github.orangewest.easytrans.demo.mybatis.entity.BaseEntity;
import io.github.orangewest.trans.repository.TransContext;
import io.github.orangewest.trans.repository.TransRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 通用数据库翻译仓库：根据 {@link io.github.orangewest.easytrans.demo.mybatis.annotation.DbTransRepo#entity()}
 * 指定的实体类，通过 {@link TransDriver} 批量按 id 查库，返回 {@code id -> 实体}。
 *
 * <p>泛型 {@code R = BaseEntity}：实际返回的是具体的实体子类（如 {@code Teacher}），
 * 运行期由 easy-trans 判定「目标字段类型可赋值」后做整体对象填充。
 */
@Component
public class DbTransRepository implements TransRepository<Long, BaseEntity> {

    @Autowired
    private TransDriver transDriver;

    @Override
    @SuppressWarnings("unchecked")
    public Map<Long, BaseEntity> getTransValueMap(List<Long> transValues, TransContext context) {
        Class<?> raw = context.get("entity", Class.class);
        if (raw == null || BaseEntity.class.equals(raw)) {
            return Map.of();
        }
        Class<? extends BaseEntity> entity = (Class<? extends BaseEntity>) raw;
        List<? extends BaseEntity> entities = transDriver.findByIds(transValues, entity);
        return entities.stream()
                .collect(Collectors.toMap(BaseEntity::getId, x -> x));
    }
}
