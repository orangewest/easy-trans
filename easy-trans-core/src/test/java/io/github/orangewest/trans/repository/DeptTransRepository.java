package io.github.orangewest.trans.repository;

import io.github.orangewest.trans.dto.DeptDto;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class DeptTransRepository implements TransRepository<String, DeptDto> {


    @Override
    public Map<String, DeptDto> getTransValueMap(List<String> transValues, Annotation transAnno) {
        return getDepts().stream()
                .filter(x -> transValues.contains(x.getCode()))
                .collect(Collectors.toMap(DeptDto::getCode, x -> x));
    }

    public List<DeptDto> getDepts() {
        List<DeptDto> deptDtos = new ArrayList<>();
        deptDtos.add(new DeptDto("1", "部门1"));
        deptDtos.add(new DeptDto("2", "部门2"));
        return deptDtos;
    }

}
