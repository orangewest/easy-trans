package io.github.orangewest.trans.spring.aot;

import io.github.orangewest.trans.annotation.DictTrans;
import io.github.orangewest.trans.annotation.Trans;
import io.github.orangewest.trans.annotation.TransRepo;
import io.github.orangewest.trans.annotation.TransRepos;
import io.github.orangewest.trans.annotation.EnumTrans;
import io.github.orangewest.trans.repository.TransRepository;
import org.jspecify.annotations.NonNull;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.asm.AnnotationVisitor;
import org.springframework.asm.ClassReader;
import org.springframework.asm.ClassVisitor;
import org.springframework.asm.FieldVisitor;
import org.springframework.asm.Opcodes;
import org.springframework.asm.Type;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.ResolvableType;
import org.springframework.util.ClassUtils;

import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 为 GraalVM Native Image 自动注册反射元数据。
 *
 * <p>easy-trans 在运行期通过反射读写用户的 DTO 字段（见 {@code ReflectUtils} / {@code TransModel}），
 * 并反射调用 {@code @Trans}/{@code @TransRepo}/{@code @DictTrans} 注解的方法（见
 * {@code TransClassMeta#parseTransRepoMetas}、{@code ReflectUtils#invokeAnnotation}）。Native image 是封闭世界，
 * 必须在构建期用 hint 声明这些反射目标，否则运行时会 {@code NoSuchFieldException} /
 * {@code InaccessibleObjectException} / 注解方法调用失败。
 *
 * <p>本 Registrar 在 Spring Boot AOT 阶段（native 构建）被自动调用，扫描类路径上
 * <b>字段级</b>标注了上述注解的 DTO，为它们（含父类字段，复刻 {@code ReflectUtils#getAllField} 的跨类遍历）
 * 注册字段读写 hint，并为注解类型本身注册方法调用 hint。这样使用 easy-trans 的 Spring Boot 应用
 * 无需手写任何 reflect-config。
 *
 * <p>关于扫描范围：默认扫描整个类路径（构建期一次性，对大型应用可能偏慢）。可通过构建期系统属性
 * {@code -Deasy-trans.aot.base-packages=com.foo,com.bar} 收敛到指定基础包。
 *
 * <p>说明：Spring Framework 7 移除了 {@code AnnotationMetadata} 的字段级元数据 API，因此这里直接用内嵌 ASM
 * （{@code org.springframework.asm}）读取类文件来判断字段注解，并递归解析元注解（支持
 * {@code @DictTrans} 以及用户自定义的「{@code @TransRepo} 元注解」注解）。
 */
public class EasyTransRuntimeHints implements RuntimeHintsRegistrar {

    /**
     * 框架自带的翻译注解（已知类，无需扫描即可注册方法调用 hint）。
     */
    private static final List<Class<?>> KNOWN_TRAN_ANNOTATIONS = Arrays.asList(
        Trans.class, TransRepo.class, DictTrans.class, TransRepos.class);

    /**
     * 已知翻译注解的 ASM 描述符集合，用于快速判定「字段注解是否直接是翻译注解」。
     */
    private static final Set<String> KNOWN_TRAN_ANNO_DESCRIPTORS;
    static {
        Set<String> set = new HashSet<>();
        for (Class<?> anno : KNOWN_TRAN_ANNOTATIONS) {
            set.add("L" + anno.getName().replace('.', '/') + ";");
        }
        KNOWN_TRAN_ANNO_DESCRIPTORS = Collections.unmodifiableSet(set);
    }

    @Override
    public void registerHints(@NonNull RuntimeHints hints, ClassLoader classLoader) {
        ClassLoader cl = (classLoader != null) ? classLoader : getClass().getClassLoader();

        // 1. 框架自带注解：注册方法调用 hint（@Trans.trans/key/using、@TransRepo.name/using、
        //    @DictTrans.name/group、@TransRepos.value 等，由 getDeclaredAnnotationsByType /
        //    ReflectUtils.invokeAnnotation 在运行期反射调用）。
        for (Class<?> anno : KNOWN_TRAN_ANNOTATIONS) {
            hints.reflection().registerType(anno, MemberCategory.INVOKE_DECLARED_METHODS);
        }

        // 2. 扫描类路径，定位「字段级标注了翻译注解」的 DTO，并收集自定义元注解。
        Set<String> customAnnoDescriptors = Collections.newSetFromMap(new ConcurrentHashMap<>());
        Set<String> repoUsingDescriptors = Collections.newSetFromMap(new ConcurrentHashMap<>());
        ClassPathScanningCandidateComponentProvider scanner =
                getClassPathScanningCandidateComponentProvider(cl, customAnnoDescriptors, repoUsingDescriptors);

        for (String basePackage : resolveBasePackages()) {
            for (BeanDefinition definition : scanner.findCandidateComponents(basePackage)) {
                String className = definition.getBeanClassName();
                if (className == null) {
                    continue;
                }
                try {
                    registerDtoHints(hints, ClassUtils.forName(className, cl));
                } catch (Throwable ex) {
                    // 扫描到无法加载的类（如损坏的 class、仅存在于特定环境的类）时跳过，不影响其余类。
                }
            }
        }

        // 3. 自定义元注解（用户自定义的、本身被 @TransRepo 元标注的注解）需要方法调用 hint：
        //    TransClassMeta.parseTransRepoMetas 通过 ReflectUtils.invokeAnnotation 反射调用其 name() 等方法。
        // 4. R6: register reflection hints for repository result types (readValueByKey reads
        //    the key field from the TransRepository<R> result object at runtime).
        registerResultClassHints(hints, cl, repoUsingDescriptors);

        for (String descriptor : customAnnoDescriptors) {
            try {
                String className = descriptor.substring(1, descriptor.length() - 1).replace('/', '.');
                Class<?> anno = ClassUtils.forName(className, cl);
                if (anno.isAnnotation()) {
                    hints.reflection().registerType(anno, MemberCategory.INVOKE_DECLARED_METHODS);
                }
            } catch (Throwable ex) {
                // 理论不会失败，忽略。
            }
        }
    }

    private static @NonNull ClassPathScanningCandidateComponentProvider getClassPathScanningCandidateComponentProvider(
            ClassLoader cl, Set<String> customAnnoDescriptors, Set<String> repoUsingDescriptors) {
        Map<String, Boolean> metaCache = new ConcurrentHashMap<>();

        ClassPathScanningCandidateComponentProvider scanner =
            new ClassPathScanningCandidateComponentProvider(false) {
                @Override
                protected boolean isCandidateComponent(@NonNull AnnotatedBeanDefinition beanDefinition) {
                    // 默认实现会排除 abstract / interface；这里放开，确保「@Trans 打在抽象父类字段上、
                    // 由具体子类实例化」的场景也能被扫描到（运行期 ReflectUtils.getAllField 会跨类遍历）。
                    return true;
                }
            };
        scanner.addIncludeFilter((reader, _) -> {
            try (InputStream is = reader.getResource().getInputStream()) {
                FieldAnnotationDetector detector =
                    new FieldAnnotationDetector(cl, customAnnoDescriptors, repoUsingDescriptors, metaCache);
                new ClassReader(is).accept(detector,
                    ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
                return detector.found();
            } catch (Throwable ex) {
                return false;
            }
        });
        return scanner;
    }

    /**
     * 解析要扫描的基础包。默认扫描整个类路径（{@code ""}）；
     * 若设置了 {@code easy-trans.aot.base-packages} 则只扫描指定包，收敛构建期开销。
     */
    private List<String> resolveBasePackages() {
        String prop = System.getProperty("easy-trans.aot.base-packages");
        if (prop != null && !prop.trim().isEmpty()) {
            return Arrays.stream(prop.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
        }
        return Collections.singletonList("");
    }

    /**
     * 为 DTO 类及其所有父类注册字段读写 hint（复刻 {@code ReflectUtils#getAllField} 的跨类遍历）。
     */
    private void registerDtoHints(RuntimeHints hints, Class<?> clazz) {
        Class<?> current = clazz;
        while (current != null && current != Object.class) {
            // ACCESS_DECLARED_FIELDS 同时覆盖字段枚举（getDeclaredFields，见 ReflectUtils.getAllField）
            // 与读写访问（field.get/set）；DECLARED_FIELDS 在 Spring Framework 7 已废弃，故只用此项。
            hints.reflection().registerType(current, MemberCategory.ACCESS_DECLARED_FIELDS);
            current = current.getSuperclass();
        }
    }

    /**
     * 用 ASM 判断一个类是否有任意字段标注了翻译注解（含自定义元注解），
     * 并把发现的自定义元注解描述符记入 {@code customAnnoDescriptors}。
     */
    private static final String TRANS_REPO_DESCRIPTOR =
            "L" + TransRepo.class.getName().replace('.', '/') + ";";
    private static final String TRANS_DESCRIPTOR =
            "L" + Trans.class.getName().replace('.', '/') + ";";
    private static final String TRANS_REPOS_DESCRIPTOR =
            "L" + TransRepos.class.getName().replace('.', '/') + ";";
    private static final String ENUM_TRANS_DESCRIPTOR =
            "L" + EnumTrans.class.getName().replace('.', '/') + ";";

    /**
     * R6: register reflection hints for the result type {@code R} of every repository that is
     * explicitly referenced via {@code using} (on @Trans / @TransRepo / a custom @TransRepo
     * meta-annotation) or via @EnumTrans's {@code enumClass()}. At runtime
     * {@code ReflectUtils.readValueByKey} reflectively reads the {@code key} field from the
     * result object, traversing superclasses, so Native Image's closed world needs these
     * declared-field hints. Repositories resolved only by name (instance registered at runtime)
     * cannot be discovered statically and must be hinted manually by the user.
     */
    private void registerResultClassHints(RuntimeHints hints, ClassLoader cl, Set<String> repoUsingDescriptors) {
        for (String descriptor : repoUsingDescriptors) {
            try {
                String className = descriptor.substring(1, descriptor.length() - 1).replace('/', '.');
                Class<?> repoClass = ClassUtils.forName(className, cl);
                if (repoClass == Void.class) {
                    continue;
                }
                if (TransRepository.class.isAssignableFrom(repoClass)) {
                    Class<?> resultType = ResolvableType.forClass(repoClass)
                            .as(TransRepository.class).resolveGeneric(1);
                    if (resultType == null || resultType == Object.class
                            || resultType.getName().startsWith("java.")
                            || resultType.getName().startsWith("javax.")) {
                        // type erasure / type variable / JDK class: cannot or need not be hinted
                        continue;
                    }
                    registerDtoHints(hints, resultType);
                } else {
                    // directly supplied class (e.g. @EnumTrans enumClass()): register its own fields
                    if (repoClass.getName().startsWith("java.")
                            || repoClass.getName().startsWith("javax.")) {
                        continue;
                    }
                    registerDtoHints(hints, repoClass);
                }
            } catch (Throwable ex) {
                // repo class only present in a specific environment, or generics not resolvable
            }
        }
    }

    private static void resolveUsingFromMetaAnnotation(String descriptor, ClassLoader cl,
                                                     Set<String> repoUsingDescriptors) {
        try {
            String className = descriptor.substring(1, descriptor.length() - 1).replace('/', '.');
            Class<?> annoClass = ClassUtils.forName(className, cl);
            if (!annoClass.isAnnotation()) {
                return;
            }
            TransRepo transRepo = annoClass.getAnnotation(TransRepo.class);
            if (transRepo != null) {
                addRepoUsing(transRepo.using(), repoUsingDescriptors);
                return;
            }
            Trans trans = annoClass.getAnnotation(Trans.class);
            if (trans != null && trans.using() != Trans.None.class) {
                addRepoUsing(trans.using(), repoUsingDescriptors);
            }
        } catch (Throwable ex) {
            // meta-annotation class could not be loaded/parsed: ignore
        }
    }

    private static void addRepoUsing(Class<?> repoClass, Set<String> repoUsingDescriptors) {
        repoUsingDescriptors.add("L" + repoClass.getName().replace('.', '/') + ";");
    }

    private static final class UsingCapturingVisitor extends AnnotationVisitor {
        private final Set<String> repoUsingDescriptors;
        UsingCapturingVisitor(Set<String> repoUsingDescriptors) {
            super(Opcodes.ASM9);
            this.repoUsingDescriptors = repoUsingDescriptors;
        }
        @Override
        public void visit(String name, Object value) {
            if (("using".equals(name) || "enumClass".equals(name)) && value instanceof Type t) {
                repoUsingDescriptors.add(t.getDescriptor());
            }
        }
    }

    private static final class TransReposUsingVisitor extends AnnotationVisitor {
        private final Set<String> repoUsingDescriptors;
        TransReposUsingVisitor(Set<String> repoUsingDescriptors) {
            super(Opcodes.ASM9);
            this.repoUsingDescriptors = repoUsingDescriptors;
        }
        @Override
        public AnnotationVisitor visitArray(String name) {
            if ("value".equals(name)) {
                return new AnnotationVisitor(Opcodes.ASM9) {
                    @Override
                    public AnnotationVisitor visitAnnotation(String n, String desc) {
                        if (TRANS_REPO_DESCRIPTOR.equals(desc)) {
                            return new UsingCapturingVisitor(repoUsingDescriptors);
                        }
                        return null;
                    }
                };
            }
            return null;
        }
    }

    private static final class FieldAnnotationDetector extends ClassVisitor {

        private final ClassLoader classLoader;
        private final Set<String> customAnnoDescriptors;
        private final Map<String, Boolean> metaCache;
        private final Set<String> repoUsingDescriptors;
        private boolean found = false;

        FieldAnnotationDetector(ClassLoader classLoader, Set<String> customAnnoDescriptors,
                                Set<String> repoUsingDescriptors, Map<String, Boolean> metaCache) {
            super(Opcodes.ASM9);
            this.classLoader = classLoader;
            this.customAnnoDescriptors = customAnnoDescriptors;
            this.repoUsingDescriptors = repoUsingDescriptors;
            this.metaCache = metaCache;
        }

        boolean found() {
            return found;
        }

        @Override
        public FieldVisitor visitField(int access, String name, String descriptor,
                                        String signature,  Object value) {
            return new FieldVisitor(Opcodes.ASM9) {
                @Override
                public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
                    if (KNOWN_TRAN_ANNO_DESCRIPTORS.contains(desc)) {
                        found = true;
                        if (TRANS_REPO_DESCRIPTOR.equals(desc) || TRANS_DESCRIPTOR.equals(desc)) {
                            return new UsingCapturingVisitor(repoUsingDescriptors);
                        }
                        if (TRANS_REPOS_DESCRIPTOR.equals(desc)) {
                            return new TransReposUsingVisitor(repoUsingDescriptors);
                        }
                        resolveUsingFromMetaAnnotation(desc, classLoader, repoUsingDescriptors);
                        return null;
                    } else if (isTransMetaAnnotation(desc, new HashSet<>())) {
                        found = true;
                        customAnnoDescriptors.add(desc);
                        resolveUsingFromMetaAnnotation(desc, classLoader, repoUsingDescriptors);
                        if (ENUM_TRANS_DESCRIPTOR.equals(desc)) {
                            return new UsingCapturingVisitor(repoUsingDescriptors);
                        }
                        return null;
                    }
                    return null;
                }
            };
        }

        /**
         * 递归判断某注解描述符是否为（直接或间接）翻译注解：
         * 本身在已知集合内，或其声明的任一元注解是翻译注解。
         */
        private boolean isTransMetaAnnotation(String descriptor, Set<String> visited) {
            if (KNOWN_TRAN_ANNO_DESCRIPTORS.contains(descriptor)) {
                return true;
            }
            if (visited.contains(descriptor)) {
                return false;
            }
            visited.add(descriptor);
            Boolean cached = metaCache.get(descriptor);
            if (cached != null) {
                return cached;
            }
            String internalName = descriptor.substring(1, descriptor.length() - 1);
            boolean[] result = {false};
            try (InputStream is = classLoader.getResourceAsStream(internalName + ".class")) {
                if (is != null) {
                    new ClassReader(is).accept(new ClassVisitor(Opcodes.ASM9) {
                        @Override
                        public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
                            if (isTransMetaAnnotation(desc, visited)) {
                                result[0] = true;
                            }
                            return null;
                        }
                    }, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
                }
            } catch (Throwable ex) {
                // 注解类字节不可读（如 JDK 内置注解）时按「非翻译注解」处理。
            }
            metaCache.put(descriptor, result[0]);
            return result[0];
        }
    }
}
