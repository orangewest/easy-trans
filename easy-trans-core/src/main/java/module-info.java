/**
 * easy-trans-core is a zero-dependency JPMS module.
 * Exports public API packages; internal implementation packages (core / util) are not exported.
 * <p>
 * 声明为 open module：整个模块对反射开放，让 JUnit Platform 在 IDE 以 module-path 方式运行测试时
 * 可反射访问含 @Test 的包（setAccessible）。相比逐包 {@code opens ... to org.junit.platform.commons}，
 * open module 不引用 test 域模块，主编译零「找不到模块」警告，也无需随新增测试包维护 opens 列表。
 * Maven Surefire 走 classpath，不依赖此声明。
 */
open module io.github.orangewest.trans {

    // 测试代码位于本模块（IDE 以 module-path 运行单测时通常通过 --patch-module 并入），
    // 需读取 JUnit Jupiter API 才能使用 @Test / @BeforeAll 等注解。用 `requires static` 声明为
    // 编译/运行期可选依赖：有 JUnit 时（测试）即满足可读性，无 JUnit 时（作为库被消费）不影响启动，
    // 从而免去 IDE 单测时必须手动加 --add-reads io.github.orangewest.trans=org.junit.jupiter.api。
    requires static org.junit.jupiter.api;

    exports io.github.orangewest.trans.annotation;
    exports io.github.orangewest.trans.repository;
    exports io.github.orangewest.trans.repository.dict;
    exports io.github.orangewest.trans.resolver;
    exports io.github.orangewest.trans.service;
    exports io.github.orangewest.trans.propagation;
    exports io.github.orangewest.trans.metrics;
    exports io.github.orangewest.trans.exception;
}
