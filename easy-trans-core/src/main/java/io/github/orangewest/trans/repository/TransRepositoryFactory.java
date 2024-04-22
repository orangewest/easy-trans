package io.github.orangewest.trans.repository;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TransRepositoryFactory {


    /**
     * key type  val是对应type的service
     */
    private final static Map<Class<? extends TransRepository>, TransRepository> TRANS_REPOSITORY_MAP = new ConcurrentHashMap<>();

    public static TransRepository getTransRepository(Class<? extends TransRepository> repository) {
        return TRANS_REPOSITORY_MAP.get(repository);
    }

    public static void register(TransRepository transRepository) {
        if (transRepository == null) {
            return;
        }
        TRANS_REPOSITORY_MAP.put(transRepository.getClass(), transRepository);
    }

}
