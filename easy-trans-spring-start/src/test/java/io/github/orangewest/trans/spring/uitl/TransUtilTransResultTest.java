package io.github.orangewest.trans.spring.uitl;

import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TransUtilTransResultTest {

    @Test
    void sync_result_is_translated_in_place() {
        AtomicReference<Object> captured = new AtomicReference<>();
        Consumer<Object> translator = captured::set;
        Object payload = new Object();

        Object result = TransUtil.transResult(payload, translator);

        assertSame(payload, result);
        assertSame(payload, captured.get());
    }

    @Test
    void null_result_is_returned_untouched() {
        AtomicReference<Object> captured = new AtomicReference<>();
        Object result = TransUtil.transResult(null, captured::set);

        assertNull(result);
        assertNull(captured.get());
    }

    @Test
    void completion_stage_translates_inner_value() throws ExecutionException, InterruptedException {
        AtomicReference<Object> captured = new AtomicReference<>();
        Consumer<Object> translator = captured::set;
        Object inner = new Object();
        CompletableFuture<Object> future = CompletableFuture.completedFuture(inner);

        Object returned = TransUtil.transResult(future, translator);

        assertTrue(returned instanceof CompletableFuture);
        Object completed = ((CompletableFuture<?>) returned).get();
        assertSame(inner, completed);
        assertSame(inner, captured.get());
    }

    @Test
    void completion_stage_translates_when_completed_later() throws ExecutionException, InterruptedException {
        AtomicReference<Boolean> called = new AtomicReference<>(false);
        Consumer<Object> translator = v -> called.set(true);
        CompletableFuture<Object> future = new CompletableFuture<>();

        Object returned = TransUtil.transResult(future, translator);
        assertFalse(called.get());

        Object inner = new Object();
        future.complete(inner);

        Object completed = ((CompletableFuture<?>) returned).get();
        assertSame(inner, completed);
        assertTrue(called.get());
    }
}
