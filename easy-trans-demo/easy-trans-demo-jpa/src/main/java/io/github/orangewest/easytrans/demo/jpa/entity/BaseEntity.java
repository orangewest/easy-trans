package io.github.orangewest.easytrans.demo.jpa.entity;

import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;

import java.io.Serializable;
import java.util.Date;

/**
 * 基础实体类：所有 JPA 实体都应继承它，复用主键与审计字段。
 * 使用 {@code @MappedSuperclass} 使其字段被子类实体继承映射。
 */
@MappedSuperclass
public abstract class BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键 id */
    @Id
    private Long id;

    /** 创建者 */
    private Long creator;

    /** 创建时间 */
    private Date createDate;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getCreator() {
        return creator;
    }

    public void setCreator(Long creator) {
        this.creator = creator;
    }

    public Date getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Date createDate) {
        this.createDate = createDate;
    }
}
