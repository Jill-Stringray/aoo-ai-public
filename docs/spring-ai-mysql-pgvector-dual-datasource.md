# Spring AI + MySQL + pgvector 双数据源标准工程结构（企业实践版）

> 目标：业务数据走 MySQL，向量检索走 PostgreSQL(pgvector)，在配置与代码层面做到“职责分离、命名明确、可扩展”。

## 1. 推荐目录结构

```text
src/main/java/com/clockshore/aooai
├─ config
│  ├─ datasource
│  │  └─ PgVectorDataSourceConfig.java   # pgvector 专用数据源配置
│  ├─ SecurityConfig.java
│  └─ ChatStreamExecutorConfig.java
├─ controller
├─ service
├─ mapper                                   # 业务 MySQL Mapper
└─ Entity

src/main/resources
└─ application.yaml

docs
└─ spring-ai-mysql-pgvector-dual-datasource.md
```

## 2. 双数据源职责划分

- `spring.datasource.*`：主数据源（MySQL），用于用户、会话、消息等业务表。
- `app.datasource.pgvector.*`：第二数据源（PostgreSQL + pgvector），用于向量库与语义检索。
- `spring.ai.vectorstore.pgvector.datasource.*`：Spring AI pgvector 的向量存储连接参数，复用 `app.datasource.pgvector.*`，避免重复维护。

## 3. 命名与配置规范（关键点）

1. **业务主库用 Spring 标准前缀**：`spring.datasource`，便于 MyBatis/JdbcTemplate 自动装配。
2. **辅助库放到 app 命名空间**：`app.datasource.pgvector`，语义清晰，不与主库混淆。
3. **连接池参数分层**：`app.datasource.pgvector.hikari.*`，后续压测时可独立调优。
4. **避免硬编码重复**：Spring AI 的 pgvector datasource 通过 `${...}` 引用 app 配置。

## 4. 当前项目已完成的落地

- 新增 `PgVectorDataSourceConfig`，提供 `pgVectorDataSourceProperties` 与 `pgVectorDataSource` Bean。
- `application.yaml` 已按“主库 + 向量库”双数据源模式重构，并支持 Hikari 连接池参数。
- Spring AI pgvector 的数据源改为引用 `app.datasource.pgvector.*`，配置更统一。

## 5. 企业项目下一步建议

- 为 pgvector 独立建 repository/dao 层（如果你后续需要手写 SQL）。
- 在 CI 中加入连接检查（MySQL、PostgreSQL 健康探针）。
- 将敏感信息迁移到环境变量或密钥系统（如 KMS、Vault），不要明文提交。
