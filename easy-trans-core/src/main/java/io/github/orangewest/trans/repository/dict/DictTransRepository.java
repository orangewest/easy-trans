package io.github.orangewest.trans.repository.dict;

import io.github.orangewest.trans.annotation.DictTrans;
import io.github.orangewest.trans.core.TransResult;
import io.github.orangewest.trans.repository.TransRepository;

import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DictTransRepository implements TransRepository<String, Map<String, String>> {

    private final DictLoader dictLoader;

    public DictTransRepository(DictLoader dictLoader) {
        this.dictLoader = dictLoader;
    }

    @Override
    public List<TransResult<String, Map<String, String>>> getTransValueList(List<String> transValues, Annotation transAnno) {
        if (dictLoader != null && transAnno instanceof DictTrans) {
            DictTrans dictTrans = (DictTrans) transAnno;
            String group = dictTrans.group();
            return Stream.of(group)
                    .map(x -> TransResult.of(x, dictLoader.loadDict(x)))
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

}
