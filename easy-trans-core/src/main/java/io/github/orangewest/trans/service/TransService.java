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

public class TransService {

    private volatile ExecutorService executor;

    public void setExecutor(ExecutorService executor) {
        this.executor = executor;
    }

    /**
     * 懒初始化虚拟线程执行器：未通过 {@link #setExecutor} 显式指定时，
     * 首次翻译按需创建（每个任务一条虚拟线程）。无需调用方预先 init()。
     */
    private ExecutorService getExecutor() {
        ExecutorService e = executor;
        if (e == null) {
            synchronized (this) {
                e = executor;
                if (e == null) {
                    e = Executors.newThreadPerTaskExecutor(
                            Thread.ofVirtual().name("trans-", 0).factory());
                    executor = e;
                }
            }
        }
        return e;
    }

    /**
     * @param obj 需要被翻译的对象或返回值（支持同步对象、{@code Result} 等包装、{@code CompletableFuture}、
     *            {@code Mono}/{@code Flux} 等——由已注册的 {@link TransValueResolver} 适配）
     * @return 翻译后的对象（入参原对象，已就地填充翻译字段；obj 为 null 时返回 null）
     */
    public Object trans(Object obj) {
        if (obj == null) {
            return null;
        }
        TransValueResolver adapter = TransValueResolverFactory.firstSupports(obj);
        if (adapter != null) {
            return adapter.handle(obj, this::trans);
        }
        return translateCore(obj);
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
        TransMetricContext translateContext = TransMetricContext.builder(TransMetricsOperations.TRANSLATE)
                .targetClass(objClass)
                .depth(0)
                .build();
        TransMetrics.Span translateSpan = TransMetricsCollector.get()
                .startSpan(TransMetricsOperations.TRANSLATE, translateContext);
        try {
            TransClassMeta info = TransClassMetaCacheManager.getTransClassMeta(objClass);
            if (!info.needTrans()) {
                return obj;
            }
            doTrans(objList, info.getTransFieldList(), translateSpan, objClass, 0);
            return obj;
        } catch (Throwable t) {
            translateSpan.recordException(t);
            throw t;
        } finally {
            translateSpan.end();
        }
    }

    private void doTrans(List<Object> objList, List<TransFieldMeta> transFieldMetaList,
                         TransMetrics.Span parentSpan, Class<?> targetClass, int parentDepth) {
        Map<TransRepoMeta, List<TransFieldMeta>> listMap = transFieldMetaList.stream().collect(Collectors.groupingBy(TransFieldMeta::getTransRepoMeta));
        if (listMap.size() > 1) {
            List<Throwable> errors = Collections.synchronizedList(new ArrayList<>());
            CompletableFuture.allOf(
                            listMap.entrySet()
                                    .stream()
                                    .map(entry -> CompletableFuture.runAsync(() -> {
                                        try {
                                            doTrans(objList, entry.getKey(), entry.getValue(), parentSpan, targetClass, parentDepth);
                                        } catch (Throwable t) {
                                            errors.add(t);
                                        }
                                    }, getExecutor()))
                                    .toArray(CompletableFuture[]::new))
                    .join();
            if (!errors.isEmpty()) {
                Throwable cause = errors.getFirst();
                throw new TransException("Translation failed: " + cause.getMessage(), cause);
            }
        } else {
            listMap.forEach((transRepoMeta, transFields) ->
                    doTrans(objList, transRepoMeta, transFields, parentSpan, targetClass, parentDepth));
        }
    }

    private void doTrans(List<Object> objList, TransRepoMeta transRepoMeta, List<TransFieldMeta> transFields,
                         TransMetrics.Span parentSpan, Class<?> targetClass, int parentDepth) {
        // 获取翻译仓库
        TransRepository<Object, Object> transRepository = TransRepositoryFactory.getTransRepository(transRepoMeta.getRepository());
        if (transRepository == null) {
            throw new TransException("TransRepository is not registered: "
                    + (transRepoMeta.getRepository() == null ? "null" : transRepoMeta.getRepository().getName())
                    + ". Register it via TransRepositoryFactory.register(...) or mark it as a @Component in Spring.");
        }
        int repoDepth = parentDepth + 1;
        TransMetrics.Span repoSpan = startRepoSpan(transRepoMeta, targetClass, parentSpan, repoDepth);
        try {
            List<TransModel> needTransList = toNeedTransList(objList, transFields);
            if (CollectionUtils.isNotEmpty(needTransList)) {
                Map<Object, Object> transValueMap = transRepository.getTransValueMap(
                        needTransList.stream().map(TransModel::getMultipleTransVal).flatMap(Collection::stream).distinct().collect(Collectors.toList()),
                        new DefaultTransContext(transRepoMeta.getRepoName(), transRepoMeta.getAttributes(),
                                transRepoMeta.getRepoField() != null ? transRepoMeta.getRepoField().getType() : null));
                if (CollectionUtils.isNotEmpty(transValueMap)) {
                    Map<TransFieldMeta, List<TransModel>> byField = needTransList.stream()
                            .collect(Collectors.groupingBy(TransModel::getTransField));
                    int fieldDepth = repoDepth + 1;
                    for (TransFieldMeta transField : transFields) {
                        List<TransModel> fieldModels = byField.get(transField);
                        if (CollectionUtils.isEmpty(fieldModels)) {
                            continue;
                        }
                        TransMetrics.Span fieldSpan = startFieldSpan(transField, transRepoMeta, targetClass, repoSpan, fieldDepth);
                        try {
                            fieldModels.forEach(transModel -> transModel.fillValue(transValueMap));
                            if (CollectionUtils.isNotEmpty(transField.getChildren())) {
                                // 嵌套翻译：parent 指向上层 field Span，depth 递增
                                doTrans(objList, transField.getChildren(), fieldSpan, targetClass, fieldDepth);
                            }
                        } catch (Throwable t) {
                            fieldSpan.recordException(t);
                            throw t;
                        } finally {
                            fieldSpan.end();
                        }
                    }
                }
            }
        } catch (Throwable t) {
            repoSpan.recordException(t);
            throw t;
        } finally {
            repoSpan.end();
        }
    }

    private TransMetrics.Span startRepoSpan(TransRepoMeta transRepoMeta, Class<?> targetClass,
                                            TransMetrics.Span parentSpan, int depth) {
        TransMetricContext context = TransMetricContext.builder(TransMetricsOperations.REPOSITORY)
                .parent(parentSpan)
                .depth(depth)
                .targetClass(targetClass)
                .repoName(transRepoMeta.getRepoName())
                .repositoryClass(transRepoMeta.getRepository())
                .build();
        return TransMetricsCollector.get().startSpan(TransMetricsOperations.REPOSITORY, context);
    }

    private TransMetrics.Span startFieldSpan(TransFieldMeta transField, TransRepoMeta transRepoMeta, Class<?> targetClass,
                                             TransMetrics.Span parentSpan, int depth) {
        TransMetricContext context = TransMetricContext.builder(TransMetricsOperations.FIELD)
                .parent(parentSpan)
                .depth(depth)
                .targetClass(targetClass)
                .fieldName(transField.getField().getName())
                .repoName(transRepoMeta.getRepoName())
                .repositoryClass(transRepoMeta.getRepository())
                .build();
        return TransMetricsCollector.get().startSpan(TransMetricsOperations.FIELD, context);
    }

    /**
     * 获取需要翻译的集合
     *
     * @param objList     需要被翻译的对象集合
     * @param toTransList 需要被翻译的属性
     * @return 需要被翻译的集合Map<trans, List < TransModel>>
     */
    private List<TransModel> toNeedTransList(List<Object> objList, List<TransFieldMeta> toTransList) {
        return toTransList.stream()
                .flatMap(x -> objList.stream().map(o -> new TransModel(o, x)))
                .filter(TransModel::needTrans)
                .collect(Collectors.toList());
    }

}


