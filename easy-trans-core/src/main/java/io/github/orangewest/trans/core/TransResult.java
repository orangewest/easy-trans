package io.github.orangewest.trans.core;

import io.github.orangewest.trans.util.ReflectUtils;

import java.util.Objects;

public class TransResult<T, R> {

    private final T transVal;

    private final R transResult;

    private final boolean isPrimitiveResult;

    private TransResult(T transVal, R transResult) {
        Objects.requireNonNull(transVal);
        Objects.requireNonNull(transResult);
        this.transVal = transVal;
        this.transResult = transResult;
        this.isPrimitiveResult = ReflectUtils.isPrimitiveWrapper(transResult.getClass()) ||
                transResult.getClass().isPrimitive() || transResult.getClass() == String.class;
    }

    public static <T, R> TransResult<T, R> of(T transVal, R transResult) {
        return new TransResult<>(transVal, transResult);
    }

    public T getTransVal() {
        return transVal;
    }

    public R getTransResult() {
        return transResult;
    }

    public boolean isPrimitiveResult() {
        return isPrimitiveResult;
    }
    
}
