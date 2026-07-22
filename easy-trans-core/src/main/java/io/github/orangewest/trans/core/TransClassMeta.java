package io.github.orangewest.trans.core;

import io.github.orangewest.trans.annotation.Trans;
import io.github.orangewest.trans.annotation.TransRepo;
import io.github.orangewest.trans.exception.TransException;
import io.github.orangewest.trans.repository.TransRepository;
import io.github.orangewest.trans.util.ReflectUtils;
import io.github.orangewest.trans.util.StringUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;

/**
 * 需要翻译的类源信息
 */
public class TransClassMeta {

    private final Class<?> clazz;

    private List<TransFieldMeta> transFieldMetaList = new ArrayList<>();

    private final Map<String, TransRepoMeta> transRepoMetaMap = new HashMap<>();

    public TransClassMeta(Class<?> clazz) {
        this.clazz = clazz;
        parseTransRepo();
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
        Map<String, Field> fieldNameMap = declaredFields.stream().collect(Collectors.toMap(Field::getName, x -> x, (o, _) -> o));
        List<TransFieldMeta> transFieldMetas = new ArrayList<>();
        // 循环遍历所有的属性进行判断
        for (Field field : declaredFields) {
            int mod = field.getModifiers();
            // 如果是 static, final, transient 的字段，则直接跳过
            if (Modifier.isStatic(mod) || Modifier.isFinal(mod) || Modifier.isTransient(mod)) {
                continue;
            }
            // 遍历字段上声明的所有注解，识别「直接 @Trans」与「元标注了 @Trans 的自定义注解」
            for (Annotation declaredAnnotation : field.getDeclaredAnnotations()) {
                TransLike transLike = resolveTransLike(declaredAnnotation);
                if (transLike == null) {
                    continue;
                }
                String trans = transLike.trans();
                String key = transLike.key();
                Class<? extends TransRepository<?, ?>> using = transLike.using();
                if (using != Trans.None.class) {
                    Field transField = fieldNameMap.get(trans);
                    if (transField == null) {
                        throw new TransException("Field '" + field.getName() + "' declares @Trans(trans=\""
                                + trans + "\") but no such field exists in class " + clazz.getName() + ".");
                    }
                    trans = using.getName() + "#" + trans;
                    transRepoMetaMap.putIfAbsent(trans, new TransRepoMeta(transLike.trans(), transField, transLike.attributes(), using));
                }
                if (!transRepoMetaMap.containsKey(trans)) {
                    throw new TransException("Field '" + field.getName() + "' references translation repository '"
                            + trans + "' which is not declared (no @TransRepo or @Trans(using=...) found) in class "
                            + clazz.getName() + ".");
                }
                if (StringUtils.isEmpty(key)) {
                    key = field.getName();
                }
                transFieldMetas.add(new TransFieldMeta(field, key, transRepoMetaMap.get(trans)));
            }
        }
        validateMultiplicity(transFieldMetas);
        this.transFieldMetaList = buildTransTree(transFieldMetas);
    }

    /**
     * 解析字段上的某一个注解，判断它是否为「可直接当 @Trans 使用」的注解：
     * <ul>
     *   <li>直接是 {@code @Trans}；</li>
     *   <li>或它本身被 {@code @Trans} 元标注（自定义翻译注解，如 {@code @DictTrans}）。</li>
     * </ul>
     * 读取 {@code trans/key/using} 后，额外把注解<b>自身声明</b>的属性在解析期抽成 Map（自定义注解的 extra 属性，
     * 例如 {@code @DictTrans.group()}，由此进入 {@link io.github.orangewest.trans.repository.TransContext}），
     * 不递归元注解，避免覆盖/泄漏框架内部属性。
     *
     * @param annotation 字段上直接声明的一个注解
     * @return 解析出的翻译信息；非 {@code @Trans} 相关则返回 {@code null}
     */
    private static TransLike resolveTransLike(Annotation annotation) {
        if (annotation instanceof Trans trans) {
            // 直接 @Trans：其 trans/key/using 已由 TransRepoMeta 直接得出，无需（也不应）经 TransContext 暴露，
            // 与「自定义元注解才把自有属性抽进 TransContext」的约定保持一致（见 #03 invariant）。
            return new TransLike(trans.trans(), trans.key(), trans.using(), Collections.emptyMap());
        }
        // 自定义「@Trans 元注解」：其类型上元标注了 @Trans
        Trans[] metaTrans = annotation.annotationType().getDeclaredAnnotationsByType(Trans.class);
        if (metaTrans.length > 0) {
            Trans meta = metaTrans[0];
            Map<String, Object> attributes = ReflectUtils.extractAnnotationAttributes(annotation);
            // 自定义元注解若未在元 @Trans 上写死 trans/key（例如 @DbTrans 把 trans()/key() 作为自身成员、
            // 由调用处按字段差异化传入），则回退到注解自身声明的 trans()/key() 成员，
            // 使自定义 @Trans 元注解也能参数化 trans/key（同时保持向后兼容：
            // MyTrans 这类无自有成员、完全依赖元 @Trans 常量的注解行为不变）。
            String trans = meta.trans();
            if (trans == null || trans.isEmpty()) {
                Object t = attributes.get("trans");
                if (t instanceof String s && !s.isEmpty()) {
                    trans = s;
                }
            }
            String key = meta.key();
            if (key == null || key.isEmpty()) {
                Object k = attributes.get("key");
                if (k instanceof String s && !s.isEmpty()) {
                    key = s;
                }
            }
            return new TransLike(trans, key, meta.using(), attributes);
        }
        return null;
    }

    /**
     * 一个字段注解解析出的翻译信息（{@code trans/key/using} + 解析期抽取的自定义属性）。
     */
    private record TransLike(
            String trans,
            String key,
            Class<? extends TransRepository<?, ?>> using,
            Map<String, Object> attributes) {
    }

    /**
     * Multiplicity consistency check (R5): when the source field is a collection/array but the
     * target field is single-valued, the fill semantics are ambiguous (no natural "take first element"
     * convention, and it conflicts with user expectations). Fail fast at parse time instead of silently
     * filling wrong values / swallowing a ClassCastException in the write path at runtime.
     * Target field being a collection/array (or source and target both single-valued) is valid.
     */
    private void validateMultiplicity(List<TransFieldMeta> transFieldMetas) {
        for (TransFieldMeta meta : transFieldMetas) {
            Field src = meta.getTransRepoMeta().getRepoField();
            boolean srcMultiple = src != null
                    && ((Iterable.class).isAssignableFrom(src.getType()) || src.getType().isArray());
            if (srcMultiple && !meta.isMultiple()) {
                throw new TransException("Field '" + meta.getField().getName() + "' declares a translation whose "
                        + "source field '" + src.getName() + "' is a collection/array "
                        + "but the target field is single-valued. easy-trans supports source-OR-target multiplicity, "
                        + "but source-collection -> single-target is ambiguous; make the target field a collection/array, "
                        + "or resolve multiple values inside your TransRepository.");
            }
        }
    }
    private void parseTransRepo() {
        Map<String, TransRepoMeta> transRepoMetaMap = parseTransRepoMetas(this.clazz);
        List<Field> declaredFields = ReflectUtils.getAllField(this.clazz);
        for (Field field : declaredFields) {
            int mod = field.getModifiers();
            // 如果是 static, final, transient 的字段，则直接跳过
            if (Modifier.isStatic(mod) || Modifier.isFinal(mod) || Modifier.isTransient(mod)) {
                continue;
            }
            // 获取TransRepo注解
            transRepoMetaMap.putAll(parseTransRepoMetas(field));
        }
        this.transRepoMetaMap.putAll(transRepoMetaMap);
    }

    private Map<String, TransRepoMeta> parseTransRepoMetas(AnnotatedElement annotatedElement) {
        Field field = null;
        if (annotatedElement instanceof Field) {
            field = (Field) annotatedElement;
        }
        Map<String, TransRepoMeta> transRepoMetas = new HashMap<>();
        TransRepo[] transRepos = annotatedElement.getDeclaredAnnotationsByType(TransRepo.class);
        for (TransRepo transRepo : transRepos) {
            transRepoMetas.putIfAbsent(generateRepoName(transRepo.name(), field),
                    new TransRepoMeta(transRepo.name(), field, ReflectUtils.extractAnnotationAttributes(transRepo), transRepo.using()));
        }
        for (Annotation declaredAnnotation : annotatedElement.getDeclaredAnnotations()) {
            Class<? extends Annotation> annotationType = declaredAnnotation.annotationType();
            transRepos = annotationType.getDeclaredAnnotationsByType(TransRepo.class);
            for (TransRepo transRepo : transRepos) {
                String repoName = StringUtils.isNotEmpty(transRepo.name()) ? transRepo.name() : (String) ReflectUtils.invokeAnnotation(annotationType, declaredAnnotation, "name");
                transRepoMetas.putIfAbsent(generateRepoName(repoName, field),
                        new TransRepoMeta(repoName, field, ReflectUtils.extractAnnotationAttributes(declaredAnnotation), transRepo.using()));
            }
        }
        return transRepoMetas;
    }

    private String generateRepoName(String repoName, Field field) {
        if (StringUtils.isEmpty(repoName) && field != null) {
            repoName = field.getName();
        }
        return repoName;
    }

    /**
     * 构建翻译树
     */
    private List<TransFieldMeta> buildTransTree(List<TransFieldMeta> transFieldMetas) {
        Map<String, List<TransFieldMeta>> tempMap = transFieldMetas.stream().collect(Collectors.groupingBy(x -> x.getTransRepoMeta().getRepoName()));
        Map<String, List<TransFieldMeta>> nameMap = transFieldMetas.stream().collect(Collectors.groupingBy(x -> x.getField().getName()));

        return transFieldMetas.stream()
                .filter(m -> !nameMap.containsKey(m.getTransRepoMeta().getRepoName()))
                .peek(m -> {
                    Set<TransFieldMeta> visited = Collections.newSetFromMap(new IdentityHashMap<>());
                    findChildren(Collections.singletonList(m), tempMap, visited);
                })
                .collect(toList());
    }

    private void findChildren(List<TransFieldMeta> root, Map<String, List<TransFieldMeta>> tempMap, Set<TransFieldMeta> visited) {
        root.stream()
                .filter(x -> tempMap.containsKey(x.getField().getName()))
                .forEach(x -> {
                    if (!visited.add(x)) {
                        throw new TransException("Circular translation reference detected at field '"
                                + x.getField().getName() + "' in class " + clazz.getName()
                                + ". Please check @Trans/@TransRepo configuration to avoid loops.");
                    }
                    List<TransFieldMeta> children = tempMap.get(x.getField().getName());
                    x.setChildren(children);
                    findChildren(children, tempMap, visited);
                });
    }

    /**
     * @return 判断是否需要翻译
     */
    public boolean needTrans() {
        return !transFieldMetaList.isEmpty();
    }

}
