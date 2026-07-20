/**
 * easy-trans-core 是零第三方依赖的 JPMS 模块。
 * 仅导出公开 API 包；内部实现包（core / util）不导出。
 */
module io.github.orangewest.trans {

    exports io.github.orangewest.trans.annotation;
    exports io.github.orangewest.trans.repository;
    exports io.github.orangewest.trans.repository.dict;
    exports io.github.orangewest.trans.resolver;
    exports io.github.orangewest.trans.service;
    exports io.github.orangewest.trans.metrics;
    exports io.github.orangewest.trans.exception;
    exports io.github.orangewest.trans.manager;

}
