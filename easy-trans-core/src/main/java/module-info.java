/**
 * easy-trans-core is a zero-dependency JPMS module.
 * Exports public API packages; internal implementation packages (core / util) are not exported.
 */
module io.github.orangewest.trans {

    exports io.github.orangewest.trans.annotation;
    exports io.github.orangewest.trans.repository;
    exports io.github.orangewest.trans.repository.dict;
    exports io.github.orangewest.trans.resolver;
    exports io.github.orangewest.trans.service;
    exports io.github.orangewest.trans.metrics;
    exports io.github.orangewest.trans.exception;

    // Open test packages for JUnit Platform reflective access in IDE (module-path mode).
    // Maven Surefire runs tests on the classpath and does not need these.
    opens io.github.orangewest.trans.service to org.junit.platform.commons;
    opens io.github.orangewest.trans.core to org.junit.platform.commons;
    opens io.github.orangewest.trans.manager to org.junit.platform.commons;
    opens io.github.orangewest.trans.metrics to org.junit.platform.commons;
    opens io.github.orangewest.trans.resolver to org.junit.platform.commons;
    opens io.github.orangewest.trans.repository.enumdict to org.junit.platform.commons;
    opens io.github.orangewest.trans.util to org.junit.platform.commons;
}
