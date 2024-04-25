package io.github.orangewest.trans.dto;

import io.github.orangewest.trans.annotation.DictTrans;
import io.github.orangewest.trans.annotation.Trans;
import io.github.orangewest.trans.repository.SubjectTransRepository;
import io.github.orangewest.trans.repository.TeacherTransRepository;
import lombok.Data;

@Data
public class UserDto {

    private Long id;

    private String name;

    private String sex;

    @DictTrans(trans = "sex", group = "sexDict")
    private String sexName;

    private String job;

    @DictTrans(trans = "job", group = "jobDict")
    private String jobName;

    private Long teacherId;

    @Trans(trans = "teacherId", key = "name", using = TeacherTransRepository.class)
    private String teacherName;

    @Trans(trans = "teacherId", key = "subjectId", using = TeacherTransRepository.class)
    private Long subjectId;

    @Trans(trans = "subjectId", using = SubjectTransRepository.class, key = "name")
    private String subjectName;

    public UserDto(Long id, String name, Long teacherId, String sex, String job) {
        this.id = id;
        this.name = name;
        this.teacherId = teacherId;
        this.sex = sex;
        this.job = job;
    }
}
