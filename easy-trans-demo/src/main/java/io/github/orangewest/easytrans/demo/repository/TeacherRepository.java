package io.github.orangewest.easytrans.demo.repository;

import io.github.orangewest.trans.repository.TransRepository;
import io.github.orangewest.easytrans.demo.dto.TeacherDto;
import org.springframework.stereotype.Component;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

@Component
public class TeacherRepository implements TransRepository<Integer, TeacherDto> {

    @Override
    public Map<Integer, TeacherDto> getTransValueMap(List<Integer> values, Annotation anno) {
        Map<Integer, TeacherDto> result = new HashMap<>();
        for (Integer v : values) {
            TeacherDto teacher = new TeacherDto();
            teacher.setId(v);
            teacher.setName("老师" + v);
            teacher.setSex(v);
            result.put(v, teacher);
        }
        return result;
    }
}
