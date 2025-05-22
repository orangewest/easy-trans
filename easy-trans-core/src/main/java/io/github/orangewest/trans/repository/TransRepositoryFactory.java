package io.github.orangewest.trans.repository;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TransRepositoryFactory {


    /**
     * key type  val是对应type的service
     */
    @SuppressWarnings("rawtypes")
    private final static Map<Class<? extends TransRepository>, TransRepository<?, ?>> TRANS_REPOSITORY_MAP = new ConcurrentHashMap<>();

    @SuppressWarnings("unchecked")
    public static TransRepository<Object, Object> getTransRepository(Class<? extends TransRepository<?, ?>> repository) {
        return (TransRepository<Object, Object>) TRANS_REPOSITORY_MAP.get(repository);
    }

    public static void register(TransRepository<?, ?> transRepository) {
        if (transRepository == null) {
            return;
        }
        TRANS_REPOSITORY_MAP.put(transRepository.getClass(), transRepository);
    }

}
