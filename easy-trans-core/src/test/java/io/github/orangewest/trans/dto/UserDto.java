package io.github.orangewest.trans.dto;

import io.github.orangewest.trans.annotation.DictTrans;
import io.github.orangewest.trans.annotation.Trans;
import io.github.orangewest.trans.annotation.TransRepo;
import io.github.orangewest.trans.repository.DeptTransRepository;
import io.github.orangewest.trans.repository.SubjectTransRepository;
import io.github.orangewest.trans.repository.TeacherTransRepository;

public class UserDto {

    private Long id;

    private String name;

    private String sex;

    @DictTrans(group = "sexDict", trans = "sex")
    private String sexName;

    private String job;

    @DictTrans(group = "jobDict", trans = "job")
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

    public Long getId() {
        return id;
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

    public String getJob() {
        return job;
    }

    public String getJobName() {
        return jobName;
    }

    public Long getTeacherId() {
        return teacherId;
    }

    public String getTeacherName() {
        return teacherName;
    }

    public Long getSubjectId() {
        return subjectId;
    }

    public String getSubjectName() {
        return subjectName;
    }

    public String getDeptCode() {
        return deptCode;
    }

    public String getDeptName() {
        return deptName;
    }
}
