package io.github.orangewest.trans.spring.resolver;

import io.github.orangewest.trans.resolver.TransValueResolverFactory;
import io.github.orangewest.trans.annotation.Trans;
import io.github.orangewest.trans.repository.TransContext;
import io.github.orangewest.trans.repository.TransRepository;
import io.github.orangewest.trans.repository.TransRepositoryFactory;
import io.github.orangewest.trans.service.TransService;
import io.github.orangewest.trans.spring.resolver.ReactorTransResolver;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 验证「值解析」（原 trans 方法里的适配器分发）：把 {@code Result}/{@code CompletableFuture}/
 * {@code Mono}/{@code Flux} 这类包装/异步值解析为可翻译的真实对象，且翻译真实跑通并产生具体值。
 *
 * <p>引擎 {@link TransService#trans(Object)} 负责适配器分发与递归翻译，本测试直接走引擎，
 * 不再在 {@link io.github.orangewest.trans.spring.uitl.TransUtil} 里复刻一遍分发逻辑。
 */
class TransValueResolverTest {

    static TransService transService;

    @BeforeAll
    static void registerRepo() {
        TransRepositoryFactory.register(new NameRepo());
        transService = new TransService();
        // reactor 位于 classpath 时手动注册响应式解析器（生产环境由 Spring @ConditionalOnClass 注入）
        TransValueResolverFactory.register(new ReactorTransResolver());
    }

    @Test
    void sync_value_is_translated_in_place() {
        ReactiveUserDto dto = new ReactiveUserDto(1L);

        transService.trans(dto);

        assertEquals("name1", dto.name);
    }

    @Test
    void null_result_is_returned_untouched() {
        assertNull(transService.trans(null));
    }

    @Test
    void completion_stage_translates_inner_value() throws ExecutionException, InterruptedException {
        ReactiveUserDto inner = new ReactiveUserDto(1L);
        CompletableFuture<ReactiveUserDto> future = CompletableFuture.completedFuture(inner);

        Object returned = transService.trans(future);

        assertTrue(returned instanceof CompletableFuture);
        ReactiveUserDto completed = ((CompletableFuture<ReactiveUserDto>) returned).get();
        assertEquals("name1", completed.name);
    }

    @Test
    void completion_stage_translates_when_completed_later() throws ExecutionException, InterruptedException {
        CompletableFuture<ReactiveUserDto> future = new CompletableFuture<>();

        Object returned = transService.trans(future);
        assertFalse(((CompletableFuture<?>) returned).isDone());

        future.complete(new ReactiveUserDto(2L));

        ReactiveUserDto completed = ((CompletableFuture<ReactiveUserDto>) returned).get();
        assertEquals("name2", completed.name);
    }

    @Test
    void mono_actual_translation_produces_concrete_value() {
        Mono<ReactiveUserDto> mono = Mono.just(new ReactiveUserDto(1L));

        Object returned = transService.trans(mono);

        assertTrue(returned instanceof Mono);
        ReactiveUserDto result = ((Mono<ReactiveUserDto>) returned).block();
        assertEquals("name1", result.name);
    }

    @Test
    void flux_actual_translation_produces_concrete_values_per_element() {
        Flux<ReactiveUserDto> flux = Flux.just(new ReactiveUserDto(1L), new ReactiveUserDto(2L));

        Object returned = transService.trans(flux);

        assertTrue(returned instanceof Flux);
        List<ReactiveUserDto> list = ((Flux<ReactiveUserDto>) returned).collectList().block();
        assertEquals("name1", list.get(0).name);
        assertEquals("name2", list.get(1).name);
    }

    // ---- 真实翻译所需的样例类型（不实际涉及 WebFlux，仅验证响应式包装透传翻译） ----

    static class NameVal {
        public String name;

        NameVal(String name) {
            this.name = name;
        }
    }

    static class NameRepo implements TransRepository<Long, NameVal> {
        @Override
        public Map<Long, NameVal> getTransValueMap(List<Long> values, TransContext context) {
            Map<Long, NameVal> map = new HashMap<>();
            for (Long v : values) {
                map.put(v, new NameVal("name" + v));
            }
            return map;
        }
    }

    static class ReactiveUserDto {
        public Long id;

        @Trans(using = NameRepo.class, trans = "id", key = "name")
        public String name;

        ReactiveUserDto(Long id) {
            this.id = id;
        }
    }
}
