package io.github.orangewest.trans.dto;

import io.github.orangewest.trans.annotation.DictTrans;
import io.github.orangewest.trans.annotation.TransNest;

public class NestParentDto {

    private String name;

    private String sex;

    @DictTrans(group = "sexDict", trans = "sex")
    private String sexName;

    @TransNest
    private NestChildDto child;

    public NestParentDto() {
    }

    public NestParentDto(String name, String sex) {
        this.name = name;
        this.sex = sex;
    }

    public String getName() {
        return name;
    }

    public String getSex() {
        return sex;
    }

    public String getSexName() {
        return sexName;
    }

    public void setSexName(String sexName) {
        this.sexName = sexName;
    }

    public NestChildDto getChild() {
        return child;
    }

    public void setChild(NestChildDto child) {
        this.child = child;
    }

}
