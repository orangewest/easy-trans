package io.github.orangewest.trans.dto;

import io.github.orangewest.trans.annotation.Trans;
import io.github.orangewest.trans.repository.TeacherTrans2Repository;
import io.github.orangewest.trans.repository.TeacherTransRepository;
import lombok.Data;

@Data
public class UserDto3 {

    private Long id;

    private String name;

    private Long teacherId;

    @Trans(trans = "teacherId", using = TeacherTransRepository.class)
    private TeacherDto teacher;

    @Trans(trans = "teacherId", using = TeacherTrans2Repository.class)
    private Boolean isYuwenTeacher;

    public UserDto3(Long id, String name, Long teacherId) {
        this.id = id;
        this.name = name;
        this.teacherId = teacherId;
    }
}
