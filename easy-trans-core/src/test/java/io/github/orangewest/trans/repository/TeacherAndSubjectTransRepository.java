package io.github.orangewest.trans.repository;

import io.github.orangewest.trans.dto.UserDto4;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class TeacherAndSubjectTransRepository implements TransRepository<UserDto4, String> {
    @Override
    public Map<UserDto4, String> getTransValueMap(List<UserDto4> transValues, TransContext context) {
        return transValues.stream()
                .collect(Collectors.toMap(x -> x, x -> x.getTeacherId() + "#" + x.getSubjectId()));
    }

}
