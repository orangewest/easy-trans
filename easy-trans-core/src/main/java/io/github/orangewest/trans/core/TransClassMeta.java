package io.github.orangewest.trans.core;

import io.github.orangewest.trans.annotation.Trans;
import io.github.orangewest.trans.repository.TransRepository;
import io.github.orangewest.trans.util.ReflectUtils;
import io.github.orangewest.trans.util.StringUtils;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;

/**
 * 需要翻译的类源信息
 */
public class TransClassMeta implements Serializable {

    private static final long serialVersionUID = -8211850528694193388L;

    private final Class<?> clazz;

    private List<TransFieldMeta> transFieldMetaList = new ArrayList<>();

    public TransClassMeta(Class<?> clazz) {
        this.clazz = clazz;
        parseTransField();
    }

    /**
     * 获取需要翻译的字段
     *
     * @return 字段集合
     */
    public List<TransFieldMeta> getTransFieldList() {
        return this.transFieldMetaList;
    }

    private void parseTransField() {
        List<Field> declaredFields = ReflectUtils.getAllField(this.clazz);
        Map<String, Field> fieldNameMap = declaredFields.stream().collect(Collectors.toMap(Field::getName, x -> x, (o, n) -> o));
        int mod;
        List<TransFieldMeta> transFieldMetas = new ArrayList<>();
        // 循环遍历所有的属性进行判断
        for (Field field : declaredFields) {
            mod = field.getModifiers();
            // 如果是 static, final, volatile, transient 的字段，则直接跳过
            if (Modifier.isStatic(mod) || Modifier.isFinal(mod)
                    || Modifier.isVolatile(mod) || Modifier.isTransient(mod)) {
                continue;
            }

            Trans transAnno = field.getAnnotation(Trans.class);
            String trans = null;
            String key = null;
            Class<? extends TransRepository<?, ?>> repository = null;
            Annotation transAnnotation = transAnno;
            if (transAnno == null) {
                Annotation[] annotations = field.getDeclaredAnnotations();
                for (Annotation annotation : annotations) {
                    Class<? extends Annotation> annotationType = annotation.annotationType();
                    transAnno = annotationType.getAnnotation(Trans.class);
                    if (transAnno != null) {
                        repository = transAnno.using();
                        trans = StringUtils.isNotEmpty(transAnno.trans()) ? transAnno.trans() : (String) ReflectUtils.invokeAnnotation(annotationType, annotation, "trans");
                        key = StringUtils.isNotEmpty(transAnno.key()) ? transAnno.key() : (String) ReflectUtils.invokeAnnotation(annotationType, annotation, "key");
                        transAnnotation = annotation;
                        break;
                    }
                }
            } else {
                trans = transAnno.trans();
                key = transAnno.key();
                repository = transAnno.using();
            }
            if (StringUtils.isEmpty(trans)) {
                continue;
            }
            if (!fieldNameMap.containsKey(trans)) {
                continue;
            }
            if (StringUtils.isEmpty(key)) {
                key = field.getName();
            }
            transFieldMetas.add(new TransFieldMeta(field, fieldNameMap.get(trans), key, repository, transAnnotation));
        }
        this.transFieldMetaList = buildTransTree(transFieldMetas);
    }

    /**
     * 构建翻译树
     */
    private List<TransFieldMeta> buildTransTree(List<TransFieldMeta> transFieldMetas) {
        Map<String, List<TransFieldMeta>> tempMap = transFieldMetas.stream().collect(Collectors.groupingBy(TransFieldMeta::getTrans));
        Map<String, List<TransFieldMeta>> nameMap = transFieldMetas.stream().collect(Collectors.groupingBy(x -> x.getField().getName()));

        return transFieldMetas.stream()
                .filter(m -> !nameMap.containsKey(m.getTrans()))
                .peek(m -> findChildren(Collections.singletonList(m), tempMap))
                .collect(toList());
    }

    public static void findChildren(List<TransFieldMeta> root, Map<String, List<TransFieldMeta>> tempMap) {
        root.stream()
                .filter(x -> tempMap.containsKey(x.getField().getName()))
                .forEach(x -> {
                    List<TransFieldMeta> children = tempMap.get(x.getField().getName());
                    x.setChildren(children);
                    findChildren(children, tempMap);
                });
    }

    /**
     * @return 判断是否需要翻译
     */
    public boolean needTrans() {
        return !transFieldMetaList.isEmpty();
    }

}
