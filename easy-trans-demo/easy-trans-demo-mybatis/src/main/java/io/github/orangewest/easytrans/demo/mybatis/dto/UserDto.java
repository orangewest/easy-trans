package io.github.orangewest.easytrans.demo.mybatis.dto;

import io.github.orangewest.easytrans.demo.mybatis.annotation.DbTransRepo;
import io.github.orangewest.easytrans.demo.mybatis.entity.Teacher;
import io.github.orangewest.trans.annotation.Trans;

/**
 * 翻译目标 DTO：源字段 {@code teacherId} 用 {@link DbTransRepo} 绑定数据库翻译仓库，
 * 目标字段用普通 {@code @Trans(trans = "teacherId")} 引用。既支持整体对象填充（teacher），
 * 也支持按属性提取（teacherName）。
 */
public class UserDto {

    private Long id;
    private String name;

    /** 源字段：用 @DbTransRepo 绑定 DbTransRepository，并声明实体类 */
    @DbTransRepo(entity = Teacher.class)
    private Long teacherId;

    /** 整体对象填充：目标字段类型与实体一致，easy-trans 会把查到的 Teacher 整对象填入 */
    @Trans(trans = "teacherId")
    private Teacher teacher;

    /** 属性提取：目标字段为 String，按 key="name" 从 Teacher 取出 name */
    @Trans(trans = "teacherId", key = "name")
    private String teacherName;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getTeacherId() {
        return teacherId;
    }

    public void setTeacherId(Long teacherId) {
        this.teacherId = teacherId;
    }

    public Teacher getTeacher() {
        return teacher;
    }

    public void setTeacher(Teacher teacher) {
        this.teacher = teacher;
    }

    public String getTeacherName() {
        return teacherName;
    }

    public void setTeacherName(String teacherName) {
        this.teacherName = teacherName;
    }

    @Override
    public String toString() {
        return "UserDto(id=" + id + ", name=" + name
                + ", teacherId=" + teacherId + ", teacher=" + teacher
                + ", teacherName=" + teacherName + ")";
    }
}
