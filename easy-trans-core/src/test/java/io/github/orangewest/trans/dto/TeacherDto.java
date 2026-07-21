package io.github.orangewest.trans.dto;

public class TeacherDto {

    private Long id;

    private String name;

    private Long subjectId;

    public TeacherDto() {
    }

    public TeacherDto(Long id, String name, Long subjectId) {
        this.id = id;
        this.name = name;
        this.subjectId = subjectId;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Long getSubjectId() {
        return subjectId;
    }
}
