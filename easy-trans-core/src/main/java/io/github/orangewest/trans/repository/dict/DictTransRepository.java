package io.github.orangewest.trans.repository.dict;

import io.github.orangewest.trans.annotation.DictTransRepo;
import io.github.orangewest.trans.repository.TransRepository;

import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class DictTransRepository implements TransRepository<String, String> {

    private final DictLoader dictLoader;

    public DictTransRepository(DictLoader dictLoader) {
        this.dictLoader = dictLoader;
    }

    @Override
    public Map<String, String> getTransValueMap(List<String> transValues, Annotation transAnno) {
        if (dictLoader != null && transAnno instanceof DictTransRepo) {
            DictTransRepo dictTransRepo = (DictTransRepo) transAnno;
            String group = dictTransRepo.group();
            return dictLoader.loadDict(group);
        }
        return Collections.emptyMap();
    }
    
}
