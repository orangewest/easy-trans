package io.github.orangewest.trans.exception;

/**
 * 翻译过程中抛出的运行时异常。
 * 仅在 {@code TransService} 开启 strict 模式时抛出，用于把原本静默失败的情况暴露给调用方。
 */
public class TransException extends RuntimeException {

    public TransException(String message) {
        super(message);
    }

    public TransException(String message, Throwable cause) {
        super(message, cause);
    }

}
