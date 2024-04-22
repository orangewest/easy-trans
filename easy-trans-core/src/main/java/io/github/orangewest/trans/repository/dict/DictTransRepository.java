package io.github.orangewest.trans.repository.dict;

import io.github.orangewest.trans.core.TransModel;
import io.github.orangewest.trans.repository.TransRepository;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.toMap;

public class DictTransRepository implements TransRepository {

    private final DictLoader dictLoader;

    public DictTransRepository(DictLoader dictLoader) {
        this.dictLoader = dictLoader;
    }

    @Override
    public Map<Object, Object> getKeysMap(List<TransModel> transModels, List<String> keys) {
        if (dictLoader != null) {
            return keys.stream()
                    .collect(toMap(x -> x, dictLoader::loadDict));
        }
        return Collections.emptyMap();
    }

}
