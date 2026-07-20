# Changelog

## 2.0.0

> 破坏性大版本：基线从 Java 8 / Spring Boot 2.7 升级到 **JDK 25 / Spring Boot 4**。
> 仍停留在 Java 8 或 Spring Boot 2.7 的用户请继续使用 **1.x** 版本。

### 破坏性变更

- **基线升级**：编译目标 `maven.compiler.release` 由 8 提升至 **25**；最低运行环境 JRE 25，最低构建 JDK 25。
- **Spring Boot 升级**：`spring.version` 由 2.7.18 升级到 **4.1.0**；`spring-boot-starter-aop` 改为 `spring-boot-starter-aspectj`。
- **自动配置迁移**：`META-INF/spring.factories` 已移除（Boot 4 不再支持），改为
  `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`。
  未迁移则自动配置完全不生效。
- **并行执行器**：默认执行器由 `Executors.newCachedThreadPool(...)` 改为 **虚拟线程**
  （`Executors.newThreadPerTaskExecutor(Thread.ofVirtual().factory())`）。仍可通过 `TransService.setExecutor(...)` 自定义。

### 新增

- **GraalVM Native Image 支持**：`easy-trans-spring-start` 内置 `EasyTransRuntimeHints`
  （`RuntimeHintsRegistrar`），在 Spring Boot AOT 阶段自动注册翻译所需的反射元数据，无需手写 `reflect-config.json`。
- **消费方示例模块** `easy-trans-demo`：含 `@SpringBootApplication` + 示例 `TransRepository`，用于验证全链路翻译与 native 构建。

### 构建 / 发布

- 发布插件升级以兼容 JDK 25：`maven-source-plugin` 3.3.1、`maven-javadoc-plugin` 3.11.2、`maven-gpg-plugin` 3.2.7。
- 插件版本集中到根 pom 的 `<pluginManagement>` + `<properties>` 统一管理。
