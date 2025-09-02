package io.github.orangewest.trans.dto;

import io.github.orangewest.trans.TeacherTransRepo;
import io.github.orangewest.trans.annotation.Trans;
import io.github.orangewest.trans.annotation.TransRepo;
import io.github.orangewest.trans.repository.TeacherTrans2Repository;
import lombok.Data;

@Data
public class UserDto3 {

    private Long id;

    private String name;

    @TeacherTransRepo
    @TransRepo(name = "teacherId1", using = TeacherTrans2Repository.class)
    private Long teacherId;

    @Trans(trans = "teacherId")
    private TeacherDto teacher;

    @Trans(trans = "teacherId1")
    private boolean isYuwenTeacher;

    @Trans(trans = "teacherId", using = TeacherTrans2Repository.class)
    private Boolean isYuwenTeacher2;

    public UserDto3(Long id, String name, Long teacherId) {
        this.id = id;
        this.name = name;
        this.teacherId = teacherId;
    }
}
