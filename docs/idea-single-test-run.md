# IDEA 中只运行单个 JUnit 测试的方法

本项目里 `DeepSeekChatServiceIT`、`KnowledgeIngestPipelineIT`、`RetrievalIT`、`UploadIT` 都会启动完整 Spring Boot 上下文，并依赖本机 MySQL、PostgreSQL/pgvector、RabbitMQ 或本地知识库数据。它们更接近手动集成验证，不适合被普通 `mvn test` 自动全量扫描。

之前这些类使用 `*Test` / `*Tests` 命名，Maven Surefire 执行 `mvn test` 时会按默认规则自动发现它们，所以即使你本来只想从 IDEA 运行一个测试，一旦 Run Configuration 实际执行的是整个 Maven `test` goal，就会看到多个测试类依次启动。

## 已采用的项目约定

- 普通单元测试继续使用 `*Test` 或 `*Tests` 命名，可被 `mvn test` 自动执行。
- 需要外部服务、会读写数据库或会调用模型的手动集成验证使用 `*IT` 命名，默认不会被 Maven Surefire 的 `mvn test` 扫描。
- 当前已将这些手动集成验证改名为：
  - `DeepSeekChatServiceIT`
  - `KnowledgeIngestPipelineIT`
  - `RetrievalIT`
  - `UploadIT`

## 在 IDEA 中运行单个测试

推荐直接点击测试方法左侧的绿色运行按钮，并确认 Run Configuration 的目标是具体的类/方法，而不是整个包、整个模块或 Maven `test` goal。

如果 IDEA 委托 Maven 执行，需要生成带 `-Dtest=类名#方法名` 的命令，例如：

```bash
mvn -Dtest=UploadIT#testReadAndSplit test
```

不要只运行：

```bash
mvn test
```

`mvn test` 的语义是运行所有被 Surefire 自动发现的普通测试；它不是“运行当前光标所在的测试方法”。
