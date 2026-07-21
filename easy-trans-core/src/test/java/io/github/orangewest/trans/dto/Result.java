package io.github.orangewest.trans.dto;

public class Result<T> {

    private T data;

    private String message;

    public Result(T data, String message) {
        this.data = data;
        this.message = message;
    }

    public T getData() {
        return data;
    }

    public String getMessage() {
        return message;
    }
}
