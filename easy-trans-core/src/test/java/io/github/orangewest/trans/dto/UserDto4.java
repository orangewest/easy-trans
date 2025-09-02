package io.github.orangewest.trans.dto;

import io.github.orangewest.trans.annotation.Trans;
import io.github.orangewest.trans.annotation.TransRepo;
import io.github.orangewest.trans.repository.TeacherAndSubjectTransRepository;
import lombok.Data;

@Data
@TransRepo(name = "teachAndSubject", using = TeacherAndSubjectTransRepository.class)
public class UserDto4 {

    private Long id;

    private String name;

    private Long teacherId;

    private Long subjectId;

    @Trans(trans = "teachAndSubject")
    private String teacherAndSubject;

    public UserDto4(Long id, String name, Long teacherId, Long subjectId) {
        this.id = id;
        this.name = name;
        this.teacherId = teacherId;
        this.subjectId = subjectId;
    }

}
