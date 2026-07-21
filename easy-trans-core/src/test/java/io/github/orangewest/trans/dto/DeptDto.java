package io.github.orangewest.trans.dto;

public class DeptDto {

    private String code;

    private String name;

    public DeptDto() {
    }

    public DeptDto(String code, String name) {
        this.code = code;
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }
}
