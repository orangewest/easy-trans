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
import io.github.orangewest.trans.util.ReflectUtils;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

public class TransService implements AutoCloseable {

    private final TransExecutor executor = new TransExecutor();

    /**
     * Enable parallel execution when DTO has 2+ repository groups (default {@code true}).
     * <p>
     * Parallel mode runs each repo group on a virtual thread via {@link CompletableFuture#runAsync},
     * which loses the calling thread's {@link ThreadLocal} context — JPA session, Spring Security,
     * MDC trace-id, etc. If your {@code TransRepository.getTransValueMap} depends on any of these,
     * set this to {@code false} to keep all groups on the calling thread.
     */
    private volatile boolean parallelRepoGroups = true;

    /**
     * Coalesce repository queries within a single {@code trans()} call (default {@code true}, P6).
     * <p>
     * After F13 (deep cascade), the same repository can be queried multiple times in one call —
     * across nested types, the root level and repo-chain children — often with overlapping keys.
     * When enabled, results are accumulated per {@code (repository + repoName + attributes + sourceType)}
     * for the duration of the call and only <b>missing</b> keys are queried; the accumulation is
     * discarded when the call returns (request-scoped, NOT a cross-call cache).
     * <p>
     * This assumes a repository is idempotent within a single call (same key + same {@code TransContext}
     * yields the same value). If yours is not, set this to {@code false}.
     */
    private volatile boolean repoCoalescing = true;

    public void setParallelRepoGroups(boolean parallelRepoGroups) {
        this.parallelRepoGroups = parallelRepoGroups;
    }

    public void setRepoCoalescing(boolean repoCoalescing) {
        this.repoCoalescing = repoCoalescing;
    }

    public void setExecutor(ExecutorService executor) {
        this.executor.set(executor);
    }

    @Override
    public void close() {
        executor.close();
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
        return (T) translateWithCall(obj, new TransCall());
    }

    private Object translateWithCall(Object obj, TransCall call) {
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
        call.visited.addAll(objList);
        TransMetricContext ctx = TransMetricContext.builder(TransMetricsOperations.TRANSLATE)
                .targetClass(objClass)
                .build();
        TransMetrics.Span span = TransMetricsCollector.get()
                .startSpan(TransMetricsOperations.TRANSLATE, ctx);
        try {
            if (!info.getTransFieldList().isEmpty()) {
                dispatchRepoGroups(objList, info.getTransFieldList(), call, span);
            }
            if (!info.getNestFields().isEmpty()) {
                cascadeNested(objList, info.getNestFields(), call);
            }
            return obj;
        } catch (Throwable t) {
            span.recordException(t);
            throw t;
        } finally {
            span.end();
        }
    }

    // ---- dispatch ----

    private void dispatchRepoGroups(List<Object> objList, List<TransFieldMeta> transFieldMetaList,
                                     TransCall call, TransMetrics.Span parent) {
        Map<TransRepoMeta, List<TransFieldMeta>> groups = transFieldMetaList.stream()
                .collect(Collectors.groupingBy(TransFieldMeta::getTransRepoMeta));
        if (groups.size() > 1 && parallelRepoGroups) {
            List<Throwable> errors = Collections.synchronizedList(new ArrayList<>());
            CompletableFuture.allOf(
                            groups.entrySet()
                                    .stream()
                                    .map(entry -> CompletableFuture.runAsync(() -> {
                                        try {
                                            translateRepo(objList, entry.getKey(), entry.getValue(), call, parent);
                                        } catch (Throwable t) {
                                            errors.add(t);
                                        }
                                    }, executor.get()))
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
            groups.forEach((repoMeta, transFields) ->
                    translateRepo(objList, repoMeta, transFields, call, parent));
        }
    }

    private void translateRepo(List<Object> objList, TransRepoMeta transRepoMeta, List<TransFieldMeta> transFields,
                                TransCall call, TransMetrics.Span parent) {
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
        List<Object> requestedKeys = needTransList.stream()
                .map(TransModel::getMultipleTransVal).flatMap(Collection::stream).distinct().collect(Collectors.toList());
        Map<Object, Object> transValueMap = resolveValues(transRepository, transRepoMeta, requestedKeys, call, parent);
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
                dispatchRepoGroups(objList, transField.getChildren(), call, parent);
            }
        }
    }

    /**
     * Resolve translation values for {@code requestedKeys}, optionally coalescing repeated queries
     * within the current call (P6). When coalescing is off, this is a plain pass-through query.
     */
    private Map<Object, Object> resolveValues(TransRepository<Object, Object> transRepository,
                                              TransRepoMeta transRepoMeta, List<Object> requestedKeys,
                                              TransCall call, TransMetrics.Span parent) {
        Class<?> sourceType = transRepoMeta.getRepoField() != null ? transRepoMeta.getRepoField().getType() : null;
        if (!repoCoalescing) {
            return queryRepo(transRepository, transRepoMeta, requestedKeys, sourceType, parent);
        }
        CoalesceKey coalesceKey = new CoalesceKey(transRepoMeta.getRepository(), transRepoMeta.getRepoName(),
                transRepoMeta.getAttributes(), sourceType);
        ResolvedRepo resolved = call.cache.computeIfAbsent(coalesceKey, k -> new ResolvedRepo());
        synchronized (resolved) {
            List<Object> missing = requestedKeys.stream()
                    .filter(k -> !resolved.queried.contains(k))
                    .collect(Collectors.toList());
            if (!missing.isEmpty()) {
                Map<Object, Object> fetched = queryRepo(transRepository, transRepoMeta, missing, sourceType, parent);
                // 记录本批所有 missing 键为「已查过」（含 miss 键），避免后续重复查空
                resolved.queried.addAll(missing);
                if (CollectionUtils.isNotEmpty(fetched)) {
                    resolved.values.putAll(fetched);
                }
            }
            // 仅回传本次请求键命中的子集，保持与非合并路径一致的填值语义
            Map<Object, Object> out = new HashMap<>();
            for (Object key : requestedKeys) {
                Object value = resolved.values.get(key);
                if (value != null) {
                    out.put(key, value);
                }
            }
            return out;
        }
    }

    private Map<Object, Object> queryRepo(TransRepository<Object, Object> transRepository,
                                          TransRepoMeta transRepoMeta, List<Object> keys, Class<?> sourceType,
                                          TransMetrics.Span parent) {
        TransMetricContext ctx = TransMetricContext.builder(TransMetricsOperations.REPOSITORY)
                .parent(parent)
                .repoName(transRepoMeta.getRepoName())
                .repositoryClass(transRepoMeta.getRepository())
                .build();
        TransMetrics.Span span = TransMetricsCollector.get().startSpan(TransMetricsOperations.REPOSITORY, ctx);
        try {
            return transRepository.getTransValueMap(keys,
                    new DefaultTransContext(transRepoMeta.getRepoName(), transRepoMeta.getAttributes(), sourceType));
        } catch (Throwable t) {
            span.recordException(t);
            throw t;
        } finally {
            span.end();
        }
    }

    private List<TransModel> toNeedTransList(List<Object> objList, List<TransFieldMeta> toTransList) {
        return toTransList.stream()
                .flatMap(x -> objList.stream().map(o -> new TransModel(o, x)))
                .filter(TransModel::needTrans)
                .collect(Collectors.toList());
    }

    // ---- nest cascade ----

    private void cascadeNested(List<Object> objList, List<Field> nestFields, TransCall call) {
        Map<Class<?>, List<Object>> collected = new LinkedHashMap<>();
        for (Object parent : objList) {
            for (Field nestField : nestFields) {
                collectFromField(parent, nestField, collected, call);
            }
        }
        for (List<Object> sameType : collected.values()) {
            translateWithCall(sameType, call);
        }
    }

    private static void collectFromField(Object parent, Field field, Map<Class<?>, List<Object>> collected, TransCall call) {
        Object val = ReflectUtils.getFieldValue(parent, field);
        if (val == null) {
            return;
        }
        if (val instanceof Iterable<?> iterable) {
            iterable.forEach(item -> addIfTranslatable(item, collected, call));
        } else if (val.getClass().isArray()) {
            int len = Array.getLength(val);
            for (int i = 0; i < len; i++) {
                addIfTranslatable(Array.get(val, i), collected, call);
            }
        } else {
            addIfTranslatable(val, collected, call);
        }
    }

    private static void addIfTranslatable(Object item, Map<Class<?>, List<Object>> collected, TransCall call) {
        if (item == null) {
            return;
        }
        Class<?> type = item.getClass();
        if (type.getName().startsWith("java.")) {
            return;
        }
        if (!call.visited.add(item)) {
            return;
        }
        collected.computeIfAbsent(type, k -> new ArrayList<>()).add(item);
    }

    // ---- per-call context ----

    /**
     * 单次 {@code trans()} 调用的作用域上下文：环检测 visited + 仓库查询去重累积表（P6）。
     * 随调用创建、结束即丢弃，绝不跨调用复用。
     */
    private static final class TransCall {
        /** 环检测：已翻译对象（identity 语义），防对象图成环 / 自引用死循环。 */
        final Set<Object> visited = Collections.newSetFromMap(new IdentityHashMap<>());
        /** P6 去重累积表：合并键 -> 该仓库在本次调用内已解析的结果。并发分组各持不同键，竞争极少。 */
        final Map<CoalesceKey, ResolvedRepo> cache = new ConcurrentHashMap<>();
    }

    /**
     * P6 合并键：仅当仓库类、repoName、注解属性、源字段类型全一致时才复用查询结果，
     * 保证「同一查询语义」才去重（如 EnumTransRepository 依赖源字段类型推断枚举类）。
     */
    private record CoalesceKey(Class<?> repository, String repoName, Map<String, Object> attributes, Class<?> sourceType) {
    }

    /** P6 单仓库在本次调用内的解析累积：命中值 + 已查过的键（含 miss，避免重复查空）。 */
    private static final class ResolvedRepo {
        final Map<Object, Object> values = new HashMap<>();
        final Set<Object> queried = new HashSet<>();
    }

}
