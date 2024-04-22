package io.github.orangewest.trans.repository;

import io.github.orangewest.trans.core.TransModel;
import io.github.orangewest.trans.dto.TeacherDto;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class TeacherTransRepository implements TransRepository {


    @Override
    public Map<Object, Object> getTransValueMap(List<TransModel> transModels, List<Object> transValues) {
        return getTeachers().stream().collect(Collectors.toMap(TeacherDto::getId, x -> x));
    }

    public List<TeacherDto> getTeachers() {
        List<TeacherDto> teachers = new ArrayList<>();
        teachers.add(new TeacherDto(1L, "老师1", 1L));
        teachers.add(new TeacherDto(2L, "老师2", 2L));
        teachers.add(new TeacherDto(3L, "老师3", 3L));
        teachers.add(new TeacherDto(4L, "老师4", 4L));
        return teachers;
    }

}
