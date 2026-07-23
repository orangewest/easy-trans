package io.github.orangewest.easytrans.demo.repository;

import io.github.orangewest.easytrans.demo.dto.School;
import io.github.orangewest.trans.repository.TransContext;
import io.github.orangewest.trans.repository.TransRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.HashMap;
import java.util.Map;

/**
 * Repository whose result type is the plain {@code School} entity (NOT a {@code @Trans} DTO).
 * Exercises the R6 AOT hint path: {@code School} is hinted only because
 * {@code EasyTransRuntimeHints} resolves the generic {@code R} of this repository.
 */
@Component
public class SchoolRepository implements TransRepository<Integer, School> {

    @Override
    public Map<Integer, School> getTransValueMap(List<Integer> values, TransContext context) {
        Map<Integer, School> result = new HashMap<>();
        for (Integer v : values) {
            School school = new School();
            school.setId(v);
            school.setName("School-" + v);
            result.put(v, school);
        }
        return result;
    }
}
