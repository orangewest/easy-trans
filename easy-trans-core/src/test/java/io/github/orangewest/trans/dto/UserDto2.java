package io.github.orangewest.trans.dto;

import io.github.orangewest.trans.TeacherTransRepo;
import io.github.orangewest.trans.annotation.DictTransRepo;
import io.github.orangewest.trans.annotation.Trans;
import io.github.orangewest.trans.annotation.TransRepo;
import io.github.orangewest.trans.repository.SubjectTransRepository;
import lombok.Data;

import java.util.List;

@Data
public class UserDto2 {

    private Long id;

    private String name;


    @TeacherTransRepo
    private List<Long> teacherIds;

    @DictTransRepo(group = "jobDict")
    private List<String> jobIds;

    @Trans(trans = "jobIds")
    private List<String> jobNames;

    @Trans(trans = "teacherIds", key = "name")
    private List<String> teacherName;

    @Trans(trans = "teacherIds", key = "subjectId")
    @TransRepo(using = SubjectTransRepository.class)
    private List<Long> subjectIds;

    @Trans(trans = "subjectIds", key = "name")
    private List<String> subjectNames;

    public UserDto2(Long id, String name, List<Long> teacherIds, List<String> jobIds) {
        this.id = id;
        this.name = name;
        this.teacherIds = teacherIds;
        this.jobIds = jobIds;
    }
}
