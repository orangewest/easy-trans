package io.github.orangewest.easytrans.demo.repository;

import io.github.orangewest.trans.repository.TransRepository;
import org.springframework.stereotype.Component;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

@Component
public class SexRepository implements TransRepository<Integer, String> {

    @Override
    public Map<Integer, String> getTransValueMap(List<Integer> values, Annotation anno) {
        Map<Integer, String> result = new HashMap<>();
        for (Integer v : values) {
            if (v == null) {
                result.put(v, null);
            } else if (v == 1) {
                result.put(v, "男");
            } else if (v == 2) {
                result.put(v, "女");
            } else {
                result.put(v, "未知");
            }
        }
        return result;
    }
}
