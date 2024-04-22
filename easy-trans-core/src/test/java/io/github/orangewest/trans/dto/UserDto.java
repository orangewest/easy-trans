package io.github.orangewest.trans.dto;

import io.github.orangewest.trans.TeacherTrans;
import io.github.orangewest.trans.annotation.DictTrans;
import io.github.orangewest.trans.annotation.Trans;
import io.github.orangewest.trans.repository.SubjectTransRepository;
import lombok.Data;

@Data
public class UserDto {

    private Long id;

    private String name;

    private String sex;

    @DictTrans(trans = "sex", key = "sexDict")
    private String sexName;

    private String job;

    @DictTrans(trans = "job", key = "jobDict")
    private String jobName;

    private Long teacherId;

    @TeacherTrans(trans = "teacherId", key = "name")
    private String teacherName;

    @TeacherTrans(trans = "teacherId", key = "subjectId")
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
