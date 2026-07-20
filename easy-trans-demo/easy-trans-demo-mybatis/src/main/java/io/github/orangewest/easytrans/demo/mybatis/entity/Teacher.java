package io.github.orangewest.easytrans.demo.mybatis.entity;

/**
 * 数据库实体，对应 {@code teacher} 表。继承 {@link BaseEntity}，复用 id/creator/createDate。
 */
public class Teacher extends BaseEntity {

    private String name;

    private Integer sex;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getSex() {
        return sex;
    }

    public void setSex(Integer sex) {
        this.sex = sex;
    }
}
