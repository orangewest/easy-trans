package io.github.orangewest.trans.service;

import io.github.orangewest.trans.core.TransClassMeta;
import io.github.orangewest.trans.core.TransFieldMeta;
import io.github.orangewest.trans.core.TransModel;
import io.github.orangewest.trans.manager.TransClassMetaCacheManager;
import io.github.orangewest.trans.repository.TransRepository;
import io.github.orangewest.trans.repository.TransRepositoryFactory;
import io.github.orangewest.trans.resolver.TransObjResolver;
import io.github.orangewest.trans.resolver.TransObjResolverFactory;
import io.github.orangewest.trans.util.CollectionUtils;

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class TransService {

    private final static ExecutorService EXECUTORS = Executors.newCachedThreadPool(r -> new Thread(r, "trans-thread-" + r.hashCode()));

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
        Map<? extends Class<? extends TransRepository>, List<TransFieldMeta>> listMap = transFieldMetaList.stream().collect(Collectors.groupingBy(TransFieldMeta::getRepository));
        if (listMap.size() > 1) {
            CompletableFuture.allOf(
                            listMap.entrySet()
                                    .stream()
                                    .map(entry -> CompletableFuture.runAsync(() ->
                                            doTrans(objList, entry.getKey(), entry.getValue()), EXECUTORS))
                                    .toArray(CompletableFuture[]::new))
                    .join();
        } else {
            listMap.forEach((transClass, transFields) -> doTrans(objList, transClass, transFields));
        }
    }

    private void doTrans(List<Object> objList, Class<? extends TransRepository> transClass, List<TransFieldMeta> transFields) {
        TransRepository transRepository = TransRepositoryFactory.getTransRepository(transClass);
        if (transRepository == null) {
            return;
        }
        Map<String, List<TransModel>> toTransMap = getToTransMap(objList, transFields);
        if (CollectionUtils.isNotEmpty(toTransMap)) {
            doTrans_0(transRepository, toTransMap);
        }
        transFields.forEach(transField -> {
            if (CollectionUtils.isNotEmpty(transField.getChildren())) {
                doTrans(objList, transField.getChildren());
            }
        });
    }

    /**
     * 获取需要翻译的集合
     *
     * @param objList     需要被翻译的对象集合
     * @param toTransList 需要被翻译的属性
     * @return 需要被翻译的集合Map<trans, List < TransModel>>
     */
    private Map<String, List<TransModel>> getToTransMap(List<Object> objList, List<TransFieldMeta> toTransList) {
        return toTransList.stream()
                .flatMap(x -> objList.stream().map(o -> new TransModel(o, x)))
                .filter(TransModel::needTrans)
                .collect(Collectors.groupingBy(x -> x.getTransField().getTrans()));
    }

    private void doTrans_0(TransRepository transRepository, Map<String, List<TransModel>> toTransMap) {
        //分组查询
        if (toTransMap.size() > 1) {
            //说明有多个实体，异步查询
            CompletableFuture<?>[] futures = toTransMap.values()
                    .stream()
                    .map(transModels -> CompletableFuture.runAsync(() -> doTrans(transRepository, transModels), EXECUTORS))
                    .toArray(CompletableFuture[]::new);
            CompletableFuture.allOf(futures).join();

        } else {
            //直接查询
            toTransMap.values().forEach(transModels -> doTrans(transRepository, transModels));
        }
    }

    private void doTrans(TransRepository transRepository, List<TransModel> transModels) {
        List<Object> transValues = transModels.stream().map(TransModel::getMultipleTransVal).flatMap(Collection::stream).distinct().collect(Collectors.toList());
        Annotation transAnno = transModels.get(0).getTransField().getTransAnno();
        Map<Object, Object> valueMap = transRepository.getTransValueMap(transValues, transAnno);
        if (CollectionUtils.isNotEmpty(valueMap)) {
            transModels.forEach(transModel -> transModel.setValue(valueMap));
        }
//        List<String> keys = transModels.stream().map(x -> x.getTransField().getKey()).distinct().collect(Collectors.toList());
//        Map<Object, Object> keysMap = transRepository.getKeysMap(transModels, keys);
//        if (CollectionUtils.isNotEmpty(keysMap)) {
//            transModels.forEach(transModel -> transModel.setKeyValue(keysMap));
//        }
    }

}
