package io.github.orangewest.easytrans.demo.mybatis.entity;

import java.io.Serializable;
import java.util.Date;

/**
 * 基础实体类：所有数据库实体都应继承它，复用主键与审计字段。
 *
 * <p>本 demo 使用原生 MyBatis（未引入 MyBatis-Plus），故这里只是普通 POJO；
 * 若项目使用 MyBatis-Plus，可在此加上 {@code @TableId} / {@code @TableField} 等注解。
 */
public abstract class BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键 id */
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
