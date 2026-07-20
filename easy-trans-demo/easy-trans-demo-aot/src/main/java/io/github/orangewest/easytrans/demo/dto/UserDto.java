package io.github.orangewest.easytrans.demo.dto;

import io.github.orangewest.trans.annotation.DictTransRepo;
import io.github.orangewest.trans.annotation.Trans;
import io.github.orangewest.trans.annotation.TransRepo;
import io.github.orangewest.easytrans.demo.repository.SexRepository;
import io.github.orangewest.easytrans.demo.repository.TeacherRepository;

/**
 * 覆盖各翻译注解形态的示例 DTO：
 * <ul>
 *   <li>{@code @TransRepo} + {@code @Trans}（普通仓库翻译）</li>
 *   <li>{@code @DictTransRepo}（字典翻译，需要 DictLoader）</li>
 *   <li>{@code @Trans(using=...)} 整体对象填充 + 字段提取</li>
 * </ul>
 */
public class UserDto {

    private Integer id;
    private String name;

    @TransRepo(using = SexRepository.class)
    private Integer sex;

    @Trans(trans = "sex")
    private String sexName;

    @DictTransRepo(group = "sex")
    private String dictSex;

    @Trans(trans = "dictSex")
    private String dictSexName;

    @TransRepo(using = TeacherRepository.class)
    private Integer teacherId;

    @Trans(trans = "teacherId")
    private TeacherDto teacher;

    @Trans(trans = "teacherId", key = "name")
    private String teacherName;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

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

    public String getSexName() {
        return sexName;
    }

    public void setSexName(String sexName) {
        this.sexName = sexName;
    }

    public String getDictSex() {
        return dictSex;
    }

    public void setDictSex(String dictSex) {
        this.dictSex = dictSex;
    }

    public String getDictSexName() {
        return dictSexName;
    }

    public void setDictSexName(String dictSexName) {
        this.dictSexName = dictSexName;
    }

    public Integer getTeacherId() {
        return teacherId;
    }

    public void setTeacherId(Integer teacherId) {
        this.teacherId = teacherId;
    }

    public TeacherDto getTeacher() {
        return teacher;
    }

    public void setTeacher(TeacherDto teacher) {
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
                + ", sex=" + sex + ", sexName=" + sexName
                + ", dictSex=" + dictSex + ", dictSexName=" + dictSexName
                + ", teacherId=" + teacherId + ", teacher=" + teacher
                + ", teacherName=" + teacherName + ")";
    }
}
