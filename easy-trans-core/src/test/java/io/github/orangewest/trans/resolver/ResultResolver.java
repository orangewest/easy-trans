package io.github.orangewest.trans.resolver;

import io.github.orangewest.trans.dto.Result;

import java.util.function.Function;

public class ResultResolver implements TransValueResolver {
    @Override
    public boolean supports(Class<?> type) {
        return Result.class.isAssignableFrom(type);
    }

    @Override
    public Object handle(Object value, Function<Object, Object> translator) {
        Object inner = ((Result<?>) value).getData();
        // 递归走 TransService.trans：嵌套 Result<Result<T>> 也能逐层拆包并翻译
        return translator.apply(inner);
    }

}
