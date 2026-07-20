package io.github.orangewest.trans.service;

import io.github.orangewest.trans.core.TransClassMeta;
import io.github.orangewest.trans.core.TransFieldMeta;
import io.github.orangewest.trans.core.TransModel;
import io.github.orangewest.trans.core.TransRepoMeta;
import io.github.orangewest.trans.exception.TransException;
import io.github.orangewest.trans.manager.TransClassMetaCacheManager;
import io.github.orangewest.trans.metrics.TransMetrics;
import io.github.orangewest.trans.metrics.TransMetricsCollector;
import io.github.orangewest.trans.repository.TransRepository;
import io.github.orangewest.trans.repository.TransRepositoryFactory;
import io.github.orangewest.trans.repository.DefaultTransContext;
import io.github.orangewest.trans.resolver.TransObjResolver;
import io.github.orangewest.trans.resolver.TransObjResolverFactory;
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
     * @param obj 需要被翻译的对象
     * @return 是否翻译成功
     */
    public boolean trans(Object obj) {
        obj = resolveObj(obj);
        if (obj == null) {
            return false;
        }
        List<Object> objList = CollectionUtils.objToList(obj);
        if (CollectionUtils.isEmpty(objList)) {
            return false;
        }
        Class<?> objClass = objList.get(0).getClass();
        if (objClass.getName().startsWith("java.")) {
            return false;
        }
        TransMetrics.Sample sample = TransMetricsCollector.get().startTranslate();
        try {
            TransClassMeta info = TransClassMetaCacheManager.getTransClassMeta(objClass);
            if (!info.needTrans()) {
                return false;
            }
            doTrans(objList, info.getTransFieldList());
            return true;
        } catch (Throwable t) {
            sample.error(t);
            throw t;
        } finally {
            sample.stop();
        }
    }

    private Object resolveObj(Object obj) {
        if (obj == null) {
            return null;
        }
        List<TransObjResolver> resolvers = TransObjResolverFactory.getResolvers();
        boolean resolve = false;
        Object resolvedObj = obj;
        for (TransObjResolver resolver : resolvers) {
            if (resolver.support(obj)) {
                resolvedObj = resolver.resolveTransObj(obj);
                resolve = true;
                break;
            }
        }
        if (resolve) {
            resolvedObj = resolveObj(resolvedObj);
        }
        return resolvedObj;
    }

    private void doTrans(List<Object> objList, List<TransFieldMeta> transFieldMetaList) {
        Map<TransRepoMeta, List<TransFieldMeta>> listMap = transFieldMetaList.stream().collect(Collectors.groupingBy(TransFieldMeta::getTransRepoMeta));
        if (listMap.size() > 1) {
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
                Throwable cause = errors.get(0);
                throw new TransException("Translation failed: " + cause.getMessage(), cause);
            }
        } else {
            listMap.forEach((transRepoMeta, transFields) -> doTrans(objList, transRepoMeta, transFields));
        }
    }

    private void doTrans(List<Object> objList, TransRepoMeta transRepoMeta, List<TransFieldMeta> transFields) {
        // 获取翻译仓库
        TransRepository<Object, Object> transRepository = TransRepositoryFactory.getTransRepository(transRepoMeta.getRepository());
        if (transRepository == null) {
            throw new TransException("TransRepository is not registered: "
                    + (transRepoMeta.getRepository() == null ? "null" : transRepoMeta.getRepository().getName())
                    + ". Register it via TransRepositoryFactory.register(...) or mark it as a @Component in Spring.");
        }
        List<TransModel> needTransList = toNeedTransList(objList, transFields);
        if (CollectionUtils.isNotEmpty(needTransList)) {
            TransMetrics.Sample sample = TransMetricsCollector.get()
                    .startRepository(transRepoMeta.getRepoName());
            try {
                doTrans_0(transRepository, transRepoMeta, needTransList);
            } catch (Throwable t) {
                sample.error(t);
                throw t;
            } finally {
                sample.stop();
            }
        }
        for (TransFieldMeta transField : transFields) {
            if (CollectionUtils.isNotEmpty(transField.getChildren())) {
                doTrans(objList, transField.getChildren());
            }
        }
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


    private void doTrans_0(TransRepository<Object, Object> transRepository, TransRepoMeta transRepoMeta, List<TransModel> transModels) {
        List<Object> transValues = transModels.stream().map(TransModel::getMultipleTransVal).flatMap(Collection::stream).distinct().collect(Collectors.toList());
        Map<Object, Object> transValueMap = transRepository.getTransValueMap(transValues,
                new DefaultTransContext(transRepoMeta.getRepoName(), transRepoMeta.getAttributes()));
        if (CollectionUtils.isNotEmpty(transValueMap)) {
            transModels.forEach(transModel -> transModel.fillValue(transValueMap));
        }
    }

}
