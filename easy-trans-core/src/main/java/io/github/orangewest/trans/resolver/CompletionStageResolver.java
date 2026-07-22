package io.github.orangewest.trans.resolver;

import java.util.concurrent.CompletionStage;
import java.util.function.Function;

/**
 * 处理 {@code CompletableFuture} 及其实现的 {@link CompletionStage}：在结果就绪后执行翻译，
 * 而非对包装对象本身翻译（后者会静默失效）。纯 JDK 类型，无外部依赖。
 */
public class CompletionStageResolver implements TransValueResolver {

    @Override
    public boolean supports(Class<?> type) {
        return CompletionStage.class.isAssignableFrom(type);
    }

    @Override
    public Object handle(Object value, Function<Object, Object> translator) {
        return ((CompletionStage<Object>) value).thenApply(translator);
    }
}
