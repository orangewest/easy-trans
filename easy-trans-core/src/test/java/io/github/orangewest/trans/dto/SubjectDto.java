package io.github.orangewest.trans.dto;

public class SubjectDto {

    private Long id;

    private String name;

    public SubjectDto() {
    }

    public SubjectDto(Long id, String name) {
        this.id = id;
        this.name = name;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }
}
