package io.github.orangewest.trans.dto;

import io.github.orangewest.trans.annotation.DictTrans;
import io.github.orangewest.trans.annotation.TransNest;

public class NestChildDto {

    private String label;

    private String type;

    @DictTrans(group = "sexDict", trans = "type")
    private String typeName;

    @TransNest
    private NestParentDto parent;

    public NestChildDto() {
    }

    public NestChildDto(String label, String type) {
        this.label = label;
        this.type = type;
    }

    public String getLabel() {
        return label;
    }

    public String getType() {
        return type;
    }

    public String getTypeName() {
        return typeName;
    }

    public void setTypeName(String typeName) {
        this.typeName = typeName;
    }

    public NestParentDto getParent() {
        return parent;
    }

    public void setParent(NestParentDto parent) {
        this.parent = parent;
    }

}
