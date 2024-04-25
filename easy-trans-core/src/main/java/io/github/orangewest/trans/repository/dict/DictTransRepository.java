package io.github.orangewest.trans.repository.dict;

import io.github.orangewest.trans.annotation.DictTrans;
import io.github.orangewest.trans.repository.TransRepository;

import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toMap;

public class DictTransRepository implements TransRepository {

    private final DictLoader dictLoader;

    public DictTransRepository(DictLoader dictLoader) {
        this.dictLoader = dictLoader;
    }

    @Override
    public Map<Object, Object> getTransValueMap(List<Object> transValues, Annotation transAnno) {
        if (dictLoader != null && transAnno instanceof DictTrans) {
            DictTrans dictTrans = (DictTrans) transAnno;
            String group = dictTrans.group();
            return Stream.of(group).collect(toMap(x -> x, dictLoader::loadDict));
        }
        return Collections.emptyMap();
    }

}
