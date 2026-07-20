package io.github.orangewest.trans.spring.aot;

import io.github.orangewest.trans.annotation.DictTransRepo;
import io.github.orangewest.trans.annotation.Trans;
import io.github.orangewest.trans.annotation.TransRepo;
import io.github.orangewest.trans.annotation.TransRepos;
import io.github.orangewest.trans.repository.TransRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.TypeHint;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 验证 {@link EasyTransRuntimeHints} 在构建期为各类翻译注解正确生成反射 hint。
 * 这是 JVM 下的快速校验：不构建 native image，直接调用 registerHints 并断言结果。
 */
class EasyTransRuntimeHintsTest {

    /** 把扫描收敛到本测试包，避免扫描整个类路径，保证测试快速且确定性。 */
    @BeforeEach
    void narrowScan() {
        System.setProperty("easy-trans.aot.base-packages",
            "io.github.orangewest.trans.spring.aot");
    }

    @Test
    void registersReflectionHintsForAllAnnotationShapes() {
        RuntimeHints hints = new RuntimeHints();
        new EasyTransRuntimeHints().registerHints(hints, getClass().getClassLoader());

        Map<String, Set<MemberCategory>> registered = hints.reflection().typeHints()
            .collect(Collectors.toMap(h -> h.getType().getName(), TypeHint::getMemberCategories));

        // 1. DTO 类需有字段读写 hint（含 @Trans / @TransRepo / @DictTransRepo / 重复 @TransRepo / 自定义元注解）
        assertDtoFieldAccess(registered, SimpleDto.class);
        assertDtoFieldAccess(registered, DictDto.class);
        assertDtoFieldAccess(registered, RepeatRepoDto.class);
        assertDtoFieldAccess(registered, CustomMetaDto.class);

        // 2. 抽象父类的 @Trans 字段（由具体子类实例化）也要被扫描到
        assertDtoFieldAccess(registered, AbstractParent.class);

        // 3. 框架自带注解需有方法调用 hint
        assertAnnotationMethodAccess(registered, Trans.class);
        assertAnnotationMethodAccess(registered, TransRepo.class);
        assertAnnotationMethodAccess(registered, DictTransRepo.class);
        assertAnnotationMethodAccess(registered, TransRepos.class);

        // 4. 自定义「@TransRepo 元注解」也需方法调用 hint（ReflectUtils.invokeAnnotation 反射调用 name()）
        assertAnnotationMethodAccess(registered, MyRepo.class);
    }

    private void assertDtoFieldAccess(Map<String, Set<MemberCategory>> registered, Class<?> dto) {
        String key = dto.getName();
        assertTrue(registered.containsKey(key),
            "DTO 类应注册反射 hint: " + key);
        assertTrue(registered.get(key).contains(MemberCategory.ACCESS_DECLARED_FIELDS),
            "DTO 类应注册 ACCESS_DECLARED_FIELDS: " + key);
    }

    private void assertAnnotationMethodAccess(Map<String, Set<MemberCategory>> registered, Class<?> anno) {
        String key = anno.getName();
        assertTrue(registered.containsKey(key),
            "注解类型应注册反射 hint: " + key);
        assertTrue(registered.get(key).contains(MemberCategory.INVOKE_DECLARED_METHODS),
            "注解类型应注册 INVOKE_DECLARED_METHODS: " + key);
    }

    // ---- 以下为用于触发扫描的样例类（仅字段注解形态，不实际执行翻译） ----

    static class MyRepoImpl implements TransRepository<Object, Object> {
        public MyRepoImpl() {
        }

        @Override
        public java.util.Map<Object, Object> getTransValueMap(List<Object> values,
                                                              java.lang.annotation.Annotation anno) {
            return java.util.Collections.emptyMap();
        }
    }

    @Target(ElementType.FIELD)
    @Retention(RetentionPolicy.RUNTIME)
    @TransRepo(using = MyRepoImpl.class)
    @interface MyRepo {
        String name() default "";
    }

    static class SimpleDto {
        @TransRepo(using = MyRepoImpl.class)
        private String sexRepo;

        @Trans(trans = "sexRepo")
        private String sexName;
    }

    static class DictDto {
        @DictTransRepo(group = "sex")
        private String sex;
    }

    static class RepeatRepoDto {
        @TransRepo(using = MyRepoImpl.class)
        @TransRepo(using = MyRepoImpl.class)
        private String multiRepo;
    }

    static class CustomMetaDto {
        @MyRepo
        private String custom;
    }

    static abstract class AbstractParent {
        @TransRepo(using = MyRepoImpl.class)
        private String repoField;

        @Trans(trans = "repoField")
        private String translated;
    }

    static class ConcreteChild extends AbstractParent {
    }
}
