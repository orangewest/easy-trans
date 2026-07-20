package io.github.orangewest.easytrans.demo.dto;

import io.github.orangewest.trans.annotation.Trans;
import io.github.orangewest.trans.annotation.TransRepo;
import io.github.orangewest.easytrans.demo.repository.SexRepository;

/**
 * 被 {@code @Trans(using=...)} 整体填充的结果对象。
 * 自身带 {@code @TransRepo} 字段，确保被 EasyTransRuntimeHints 扫描到并注册反射 hint。
 */
public class TeacherDto {

    private Integer id;
    private String name;

    @TransRepo(using = SexRepository.class)
    private Integer sex;

    @Trans(trans = "sex")
    private String sexName;

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

    @Override
    public String toString() {
        return "TeacherDto(id=" + id + ", name=" + name + ", sex=" + sex + ", sexName=" + sexName + ")";
    }
}
