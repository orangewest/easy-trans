package io.github.orangewest.trans.spring.aot;

import io.github.orangewest.trans.annotation.DictTrans;
import io.github.orangewest.trans.annotation.Trans;
import io.github.orangewest.trans.annotation.TransRepo;
import io.github.orangewest.trans.annotation.TransRepos;
import io.github.orangewest.trans.metrics.TransMetricContext;
import io.github.orangewest.trans.metrics.TransMetrics;
import io.github.orangewest.trans.metrics.TransMetricsOperations;
import io.github.orangewest.trans.repository.TransContext;
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
import java.util.concurrent.atomic.AtomicInteger;
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

        // 1. DTO 类需有字段读写 hint（含 @Trans / @TransRepo / @DictTrans / 重复 @TransRepo / 自定义元注解）
        assertDtoFieldAccess(registered, SimpleDto.class);
        assertDtoFieldAccess(registered, DictDto.class);
        assertDtoFieldAccess(registered, RepeatRepoDto.class);
        assertDtoFieldAccess(registered, CustomMetaDto.class);
        assertDtoFieldAccess(registered, CustomTransFieldDto.class);

        // 2. 抽象父类的 @Trans 字段（由具体子类实例化）也要被扫描到
        assertDtoFieldAccess(registered, AbstractParent.class);

        // 3. 框架自带注解需有方法调用 hint
        assertAnnotationMethodAccess(registered, Trans.class);
        assertAnnotationMethodAccess(registered, TransRepo.class);
        assertAnnotationMethodAccess(registered, DictTrans.class);
        assertAnnotationMethodAccess(registered, TransRepos.class);

        // 4. 自定义「@TransRepo 元注解」需方法调用 hint（ReflectUtils.invokeAnnotation 反射调用 name()）
        assertAnnotationMethodAccess(registered, MyRepo.class);

        // 5. 自定义「@Trans 元注解」（含 extra 属性）需方法调用 hint：
        //    ReflectUtils.extractAnnotationAttributes 在解析期反射调用其自有属性（如 group()），
        //    缺少该 hint 会在 Native Image 下抛 InaccessibleObjectException。
        assertAnnotationMethodAccess(registered, MyTransField.class);
    }

    /**
     * 验证「自定义 TransMetrics 后端」在 AOT 下是干净可注册的：
     * <ul>
     *     <li>{@link TransMetricContext} 由框架在运行期经 builder 纯 Java 构造（无反射），
     *         无需任何额外 reflection hint；</li>
     *     <li>自定义后端作为普通类被实例化调用，不依赖 easy-trans 的 hint，native image 下 AOT-clean。</li>
     * </ul>
     */
    @Test
    void customTransMetricsBackendIsAotClean() {
        RuntimeHints hints = new RuntimeHints();
        new EasyTransRuntimeHints().registerHints(hints, getClass().getClassLoader());

        // 1. 自定义后端是普通类，实例化 + 调用无需任何反射 hint
        CustomBackend backend = new CustomBackend();
        TransMetricContext context = TransMetricContext.builder(TransMetricsOperations.FIELD)
                .targetClass(SimpleDto.class)
                .repoName("sexRepo")
                .fieldName("sexName")
                .depth(2)
                .build();
        TransMetrics.Span span = backend.startSpan(TransMetricsOperations.FIELD, context);
        span.end();
        assertTrue(backend.started.get() > 0, "自定义后端应被调用");
        assertEquals(2, context.getDepth());

        // 2. metrics 路径不应引入额外的 TransMetricContext reflection hint
        //    （ADR-0002：EasyTransRuntimeHints 无需为 metrics 新增 hint）
        boolean hasMetricContextHint = hints.reflection().typeHints()
                .anyMatch(h -> h.getType().getName().equals(TransMetricContext.class.getName()));
        assertTrue(!hasMetricContextHint,
                "TransMetricContext 运行期纯 Java 构造，不应需要 reflection hint");
    }

    /** 用于 AOT 校验的自定义后端：普通 Java 类，无反射依赖。 */
    static class CustomBackend implements TransMetrics {
        final AtomicInteger started = new AtomicInteger();

        @Override
        public Span startSpan(String operation, TransMetricContext context) {
            started.incrementAndGet();
            return new Span() {
                @Override
                public void setAttribute(String key, String value) {
                }

                @Override
                public void recordException(Throwable t) {
                }

                @Override
                public void end() {
                }
            };
        }
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
                                                              TransContext context) {
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
        private String sex;

        @DictTrans(group = "sex", trans = "sex")
        private String sexName;
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

    /** 自定义「@Trans 元注解」：自身被 @Trans 元标注，并携带 extra 属性 group()。 */
    @Target(ElementType.FIELD)
    @Retention(RetentionPolicy.RUNTIME)
    @Trans(trans = "sexRepo", using = MyRepoImpl.class)
    @interface MyTransField {
        String group() default "";
    }

    static class CustomTransFieldDto {
        @TransRepo(using = MyRepoImpl.class)
        private String sexRepo;

        @MyTransField(group = "sex")
        private String sexName;
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
