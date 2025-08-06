package io.github.orangewest.trans.repository;

import io.github.orangewest.trans.dto.SubjectDto;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class SubjectTransRepository implements TransRepository<Long, SubjectDto> {

    @Override
    public Map<Long, SubjectDto> getTransValueMap(List<Long> transValues, Annotation transAnno) {
        return getSubjects().stream()
                .filter(x -> transValues.contains(x.getId()))
                .collect(Collectors.toMap(SubjectDto::getId, x -> x));
    }

    public List<SubjectDto> getSubjects() {
        List<SubjectDto> subjects = new ArrayList<>();
        subjects.add(new SubjectDto(1L, "语文"));
        subjects.add(new SubjectDto(2L, "数学"));
        subjects.add(new SubjectDto(3L, "英语"));
        subjects.add(new SubjectDto(4L, "物理"));
        return subjects;
    }

}
