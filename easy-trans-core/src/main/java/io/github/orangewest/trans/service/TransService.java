package io.github.orangewest.trans.service;

import io.github.orangewest.trans.core.TransClassMeta;
import io.github.orangewest.trans.core.TransFieldMeta;
import io.github.orangewest.trans.core.TransModel;
import io.github.orangewest.trans.core.TransRepoMeta;
import io.github.orangewest.trans.exception.TransException;
import io.github.orangewest.trans.manager.TransClassMetaCacheManager;
import io.github.orangewest.trans.metrics.TransMetricContext;
import io.github.orangewest.trans.metrics.TransMetrics;
import io.github.orangewest.trans.metrics.TransMetricsCollector;
import io.github.orangewest.trans.metrics.TransMetricsOperations;
import io.github.orangewest.trans.repository.TransRepository;
import io.github.orangewest.trans.repository.TransRepositoryFactory;
import io.github.orangewest.trans.repository.DefaultTransContext;
import io.github.orangewest.trans.resolver.TransValueResolver;
import io.github.orangewest.trans.resolver.TransValueResolverFactory;
import io.github.orangewest.trans.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class TransService implements AutoCloseable {

    private volatile ExecutorService executor;

    private volatile boolean defaultExecutorCreated = false;

    /**
     * Enable parallel execution when DTO has 2+ repository groups (default {@code true}).
     * <p>
     * Parallel mode runs each repo group on a virtual thread via {@link CompletableFuture#runAsync},
     * which loses the calling thread's {@link ThreadLocal} context — JPA session, Spring Security,
     * MDC trace-id, etc. If your {@code TransRepository.getTransValueMap} depends on any of these,
     * set this to {@code false} to keep all groups on the calling thread.
     */
    private volatile boolean parallelRepoGroups = true;

    public void setParallelRepoGroups(boolean parallelRepoGroups) {
        this.parallelRepoGroups = parallelRepoGroups;
    }

    public void setExecutor(ExecutorService executor) {
        ExecutorService previous = this.executor;
        if (defaultExecutorCreated && previous != null && previous != executor) {
            previous.close();
        }
        this.executor = executor;
        this.defaultExecutorCreated = false;
    }

    private ExecutorService getExecutor() {
        ExecutorService e = executor;
        if (e == null) {
            synchronized (this) {
                e = executor;
                if (e == null) {
                    e = Executors.newThreadPerTaskExecutor(
                            Thread.ofVirtual().name("trans-", 0).factory());
                    executor = e;
                    defaultExecutorCreated = true;
                }
            }
        }
        return e;
    }

    @Override
    public void close() {
        ExecutorService e = executor;
        if (defaultExecutorCreated && e != null) {
            e.close();
            executor = null;
            defaultExecutorCreated = false;
        }
    }

    @SuppressWarnings("unchecked")
    public <T> T trans(T obj) {
        if (obj == null) {
            return null;
        }
        TransValueResolver adapter = TransValueResolverFactory.firstSupports(obj);
        if (adapter != null) {
            return (T) adapter.handle(obj, this::trans);
        }
        return (T) translateCore(obj);
    }

    private Object translateCore(Object obj) {
        List<Object> objList = CollectionUtils.objToList(obj);
        if (CollectionUtils.isEmpty(objList)) {
            return obj;
        }
        Class<?> objClass = objList.getFirst().getClass();
        if (objClass.getName().startsWith("java.")) {
            return obj;
        }
        TransClassMeta info = TransClassMetaCacheManager.getTransClassMeta(objClass);
        if (!info.needTrans()) {
            return obj;
        }
        TransMetricContext ctx = TransMetricContext.builder(TransMetricsOperations.TRANSLATE)
                .targetClass(objClass)
                .build();
        TransMetrics.Span span = TransMetricsCollector.get()
                .startSpan(TransMetricsOperations.TRANSLATE, ctx);
        try {
            doTrans(objList, info.getTransFieldList());
            return obj;
        } catch (Throwable t) {
            span.recordException(t);
            throw t;
        } finally {
            span.end();
        }
    }

    private void doTrans(List<Object> objList, List<TransFieldMeta> transFieldMetaList) {
        Map<TransRepoMeta, List<TransFieldMeta>> listMap = transFieldMetaList.stream().collect(Collectors.groupingBy(TransFieldMeta::getTransRepoMeta));
        if (listMap.size() > 1 && parallelRepoGroups) {
            List<Throwable> errors = Collections.synchronizedList(new ArrayList<>());
            CompletableFuture.allOf(
                            listMap.entrySet()
                                    .stream()
                                    .map(entry -> CompletableFuture.runAsync(() -> {
                                        try {
                                            doTrans(objList, entry.getKey(), entry.getValue());
                                        } catch (Throwable t) {
                                            errors.add(t);
                                        }
                                    }, getExecutor()))
                                    .toArray(CompletableFuture[]::new))
                    .join();
            if (!errors.isEmpty()) {
                Throwable first = errors.getFirst();
                String message = "Translation failed for " + errors.size() + " repository group(s): "
                        + errors.stream().map(Throwable::getMessage).collect(Collectors.joining(" | "));
                TransException aggregated = new TransException(message, first);
                for (int i = 1; i < errors.size(); i++) {
                    aggregated.addSuppressed(errors.get(i));
                }
                throw aggregated;
            }
        } else {
            listMap.forEach((transRepoMeta, transFields) ->
                    doTrans(objList, transRepoMeta, transFields));
        }
    }

    private void doTrans(List<Object> objList, TransRepoMeta transRepoMeta, List<TransFieldMeta> transFields) {
        TransRepository<Object, Object> transRepository = TransRepositoryFactory.getTransRepository(transRepoMeta.getRepository());
        if (transRepository == null) {
            throw new TransException("TransRepository is not registered: "
                    + (transRepoMeta.getRepository() == null ? "null" : transRepoMeta.getRepository().getName())
                    + ". Register it via TransRepositoryFactory.register(...) or mark it as a @Component in Spring.");
        }
        List<TransModel> needTransList = toNeedTransList(objList, transFields);
        if (CollectionUtils.isEmpty(needTransList)) {
            return;
        }
        Map<Object, Object> transValueMap = transRepository.getTransValueMap(
                needTransList.stream().map(TransModel::getMultipleTransVal).flatMap(Collection::stream).distinct().collect(Collectors.toList()),
                new DefaultTransContext(transRepoMeta.getRepoName(), transRepoMeta.getAttributes(),
                        transRepoMeta.getRepoField() != null ? transRepoMeta.getRepoField().getType() : null));
        if (CollectionUtils.isEmpty(transValueMap)) {
            return;
        }
        Map<TransFieldMeta, List<TransModel>> byField = needTransList.stream()
                .collect(Collectors.groupingBy(TransModel::getTransField));
        for (TransFieldMeta transField : transFields) {
            List<TransModel> fieldModels = byField.get(transField);
            if (CollectionUtils.isEmpty(fieldModels)) {
                continue;
            }
            fieldModels.forEach(transModel -> transModel.fillValue(transValueMap));
            if (CollectionUtils.isNotEmpty(transField.getChildren())) {
                doTrans(objList, transField.getChildren());
            }
        }
    }

    private List<TransModel> toNeedTransList(List<Object> objList, List<TransFieldMeta> toTransList) {
        return toTransList.stream()
                .flatMap(x -> objList.stream().map(o -> new TransModel(o, x)))
                .filter(TransModel::needTrans)
                .collect(Collectors.toList());
    }

}