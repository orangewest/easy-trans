package io.github.orangewest.easytrans.demo.jpa.driver;

import io.github.orangewest.easytrans.demo.jpa.entity.BaseEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.util.List;

/**
 * JPA 实现的 {@link TransDriver}：通过 EntityManager 按 ids 批量查询实体。
 * 实体名使用类的简单名（JPA 默认实体名），id 取自 {@link BaseEntity} 继承字段。
 */
@Component
public class JpaTransDriver implements TransDriver {

    @PersistenceContext
    private EntityManager em;

    @Override
    public List<? extends BaseEntity> findByIds(List<? extends Serializable> ids, Class<? extends BaseEntity> targetClass) {
        TypedQuery<? extends BaseEntity> query = em.createQuery(
                "SELECT e FROM " + targetClass.getSimpleName() + " e WHERE e.id IN :ids", targetClass);
        query.setParameter("ids", ids);
        return query.getResultList();
    }
}
