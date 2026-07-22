package io.github.orangewest.trans.spring.resolver;

import io.github.orangewest.trans.resolver.TransValueResolver;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.function.Function;

/**
 * 基于 Project Reactor 的 {@link TransValueResolver} 实现：直接调用 {@code Mono.map} / {@code Flux.map}
 * 在元素发射时执行翻译（同步、原地修改后返回原对象），<b>不用反射</b>。
 *
 * <p>本类引用 reactor，故为「可选依赖」路径：仅当 reactor 位于 classpath 时，由
 * {@code EasyTransAutoConfiguration} 经 {@code @ConditionalOnClass} 注入为 Spring Bean，并由
 * {@code EasyTransRegister} 注册进 {@code TransValueResolverFactory}，使 core 引擎与 {@code TransUtil}
 * 不静态依赖 reactor，保证纯 MVC 应用打 GraalVM Native 镜像不会因缺少 reactor 而构建失败。
 */
public class ReactorTransResolver implements TransValueResolver {

    @Override
    public boolean supports(Class<?> type) {
        // 按类名前缀判断（不加载类、不静态引用 reactor）：避免非 WebFlux 应用打原生镜像时因静态引用 Mono/Flux 而失败。
        return type.getName().startsWith("reactor.core.publisher.");
    }

    @Override
    public Object handle(Object value, Function<Object, Object> translator) {
        if (value instanceof Mono) {
            return ((Mono<Object>) value).map(translator);
        }
        if (value instanceof Flux) {
            return ((Flux<Object>) value).map(translator);
        }
        return value;
    }
}
