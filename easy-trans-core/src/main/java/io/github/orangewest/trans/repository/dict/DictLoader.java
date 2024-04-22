package io.github.orangewest.trans.repository.dict;


import java.util.Map;

@FunctionalInterface
public interface DictLoader {

    Map<String, String> loadDict(String dictGroup);

}
