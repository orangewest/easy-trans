package io.github.orangewest.trans.dto;

import io.github.orangewest.trans.annotation.DictTransRepo;
import io.github.orangewest.trans.annotation.Trans;
import io.github.orangewest.trans.annotation.TransRepo;
import io.github.orangewest.trans.repository.DeptTransRepository;
import io.github.orangewest.trans.repository.SubjectTransRepository;
import io.github.orangewest.trans.repository.TeacherTransRepository;
import lombok.Data;

@Data
public class UserDto {

    private Long id;

    private String name;

    @DictTransRepo(group = "sexDict")
    private String sex;

    @Trans(trans = "sex")
    private String sexName;

    @DictTransRepo(group = "jobDict")
    private String job;

    @Trans(trans = "job")
    private String jobName;

    @TransRepo(using = TeacherTransRepository.class)
    private Long teacherId;

    @Trans(trans = "teacherId", key = "name")
    private String teacherName;

    @TransRepo(using = SubjectTransRepository.class)
    @Trans(trans = "teacherId", key = "subjectId")
    private Long subjectId;

    @Trans(trans = "subjectId", key = "name")
    private String subjectName;

    private String deptCode;

    @Trans(trans = "deptCode", key = "name", using = DeptTransRepository.class)
    private String deptName;

    public UserDto(Long id, String name, Long teacherId, String sex, String job, String deptCode) {
        this.id = id;
        this.name = name;
        this.teacherId = teacherId;
        this.sex = sex;
        this.job = job;
        this.deptCode = deptCode;
    }
}
