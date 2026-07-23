package io.github.orangewest.trans.service;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * R7：验证 {@link TransService} 默认虚拟线程执行器的生命周期契约——
 * {@code close()} 只关闭*懒创建*的默认执行器，绝不触碰用户经 {@code setExecutor} 注入的执行器。
 */
class TransServiceExecutorLifecycleTest {

    @Test
    void closeMustNotShutdownUserInjectedExecutor() {
        TransService service = new TransService();
        ExecutorService userExec = Executors.newSingleThreadExecutor();
        service.setExecutor(userExec);

        service.close();

        Assertions.assertFalse(userExec.isShutdown(),
                "close() must never shut down a user-injected executor");
        userExec.shutdownNow();
    }

    @Test
    void closeIsIdempotentAndSafeOnFreshService() {
        TransService service = new TransService();
        Assertions.assertDoesNotThrow(() -> {
            service.close();
            service.close();
        });
        // 关闭后再次翻译不应出错（默认执行器可按需重建）
        Assertions.assertDoesNotThrow(() -> service.trans(new Object()));
    }
}
