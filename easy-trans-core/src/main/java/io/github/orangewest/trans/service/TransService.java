package io.github.orangewest.trans.service;

import io.github.orangewest.trans.core.TransClassMeta;
import io.github.orangewest.trans.core.TransFieldMeta;
import io.github.orangewest.trans.core.TransModel;
import io.github.orangewest.trans.core.TransRepoMeta;
import io.github.orangewest.trans.manager.TransClassMetaCacheManager;
import io.github.orangewest.trans.repository.TransRepository;
import io.github.orangewest.trans.repository.TransRepositoryFactory;
import io.github.orangewest.trans.resolver.TransObjResolver;
import io.github.orangewest.trans.resolver.TransObjResolverFactory;
import io.github.orangewest.trans.util.CollectionUtils;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class TransService {

    private ExecutorService executor;

    private volatile boolean isInit = false;

    public void setExecutor(ExecutorService executor) {
        this.executor = executor;
    }

    public void init() {
        if (this.executor == null) {
            this.executor = Executors.newCachedThreadPool(r -> new Thread(r, "trans-thread-" + r.hashCode()));
        }
        isInit = true;
    }

    /**
     * @param obj 需要被翻译的对象
     * @return 是否翻译成功
     */
    public boolean trans(Object obj) {
        if (!isInit) {
            return false;
        }
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
        TransClassMeta info = TransClassMetaCacheManager.getTransClassMeta(objClass);
        if (!info.needTrans()) {
            return false;
        }
        doTrans(objList, info.getTransFieldList());
        return true;
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
            CompletableFuture.allOf(
                            listMap.entrySet()
                                    .stream()
                                    .map(entry -> CompletableFuture.runAsync(() ->
                                            doTrans(objList, entry.getKey(), entry.getValue()), executor))
                                    .toArray(CompletableFuture[]::new))
                    .join();
        } else {
            listMap.forEach((transRepoMeta, transFields) -> doTrans(objList, transRepoMeta, transFields));
        }
    }

    private void doTrans(List<Object> objList, TransRepoMeta transRepoMeta, List<TransFieldMeta> transFields) {
        // 获取翻译仓库
        TransRepository<Object, Object> transRepository = TransRepositoryFactory.getTransRepository(transRepoMeta.getRepository());
        if (transRepository == null) {
            return;
        }
        List<TransModel> needTransList = toNeedTransList(objList, transFields);
        if (CollectionUtils.isNotEmpty(needTransList)) {
            doTrans_0(transRepository, transRepoMeta, needTransList);
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
        Map<Object, Object> transValueMap = transRepository.getTransValueMap(transValues, transRepoMeta.getRepoAnno());
        if (CollectionUtils.isNotEmpty(transValueMap)) {
            transModels.forEach(transModel -> transModel.fillValue(transValueMap));
        }
    }

}
