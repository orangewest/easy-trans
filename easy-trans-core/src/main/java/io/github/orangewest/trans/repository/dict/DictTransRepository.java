package io.github.orangewest.trans.repository.dict;

import io.github.orangewest.trans.repository.TransContext;
import io.github.orangewest.trans.repository.TransRepository;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class DictTransRepository implements TransRepository<String, String> {

    private final DictLoader dictLoader;

    public DictTransRepository(DictLoader dictLoader) {
        this.dictLoader = dictLoader;
    }

    @Override
    public Map<String, String> getTransValueMap(List<String> transValues, TransContext context) {
        if (dictLoader != null) {
            String group = context.get("group", String.class);
            if (group != null) {
                return dictLoader.loadDict(group);
            }
        }
        return Collections.emptyMap();
    }

}
