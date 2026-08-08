# AOO-AI 公司内部问答知识库数据库设计说明

## 为什么这样建库

本项目定位为**某公司内部使用**的 RAG 企业知识库系统，不是面向外部客户的 SaaS 平台。因此本次建库只围绕公司部门、员工、角色和知识资产建模，不引入面向外部客户隔离的复杂结构。

当前数据分成两类：

1. **业务主数据**：部门、员工、角色、知识库、文档、会话、审计、异步任务，放在 MySQL 的 `aoo_ai` 库中，脚本见 `aoo-ai.sql`。
2. **向量检索数据**：文档切片的 embedding，放在 PostgreSQL + pgvector 中，脚本见 `docs/database/pgvector-schema.sql`。

这样做的原因是：MySQL 更适合事务型业务数据，PostgreSQL + pgvector 更适合向量相似度检索。两边通过 `chunk_key`、`chunk_id`、`document_id`、`knowledge_base_id` 对齐。

## 关于“去掉所有索引”的处理

按当前需求，脚本已经移除所有显式二级索引、唯一索引、全文索引和 pgvector 检索索引，只保留关系型表最基本的主键与外键约束。

需要注意：在真实生产环境中，查询量上来后通常会重新评估索引，例如文档列表按知识库查询、会话列表按用户查询、任务按状态扫描、向量相似度检索等。现在先不建索引，可以让表结构更容易学习和理解；后续做压测后，再基于真实慢 SQL 添加索引，这是更贴近企业实践的演进方式。

## 核心业务链路

1. 管理员维护公司部门、员工和角色。
2. 员工创建知识库，并通过 `knowledge_base_member` 授权其他员工访问。
3. 员工上传内部文档到 `knowledge_document`。
4. 系统创建 `index_task`，异步执行解析、切分、向量化。
5. 文档切片写入 `document_chunk`，embedding 写入 pgvector 表 `knowledge_chunk_embedding`。
6. 员工发起问答，问题和回答写入 `conversation`、`conversation_message`。
7. 系统把回答引用的切片写入 `message_retrieval_source`，实现“答案可溯源”。
8. 员工反馈写入 `qa_feedback`，内部操作行为写入 `audit_log`。
9. 需要可靠异步投递时，业务事件先写入 `outbox_event`，再由后台任务投递 MQ。

## 表分层

| 层级 | 表 | 作用 |
| --- | --- | --- |
| 组织权限层 | `department`、`app_user`、`role`、`user_role` | 公司组织、员工、RBAC 角色 |
| 知识资产层 | `knowledge_base`、`knowledge_base_member`、`knowledge_document`、`document_chunk` | 知识库、授权、文档、文档切片 |
| 索引任务层 | `index_task`、`outbox_event` | 异步解析、切分、向量化、可靠事件投递 |
| 问答链路层 | `conversation`、`conversation_message`、`message_retrieval_source` | 多轮会话、消息、答案引用来源 |
| 治理运营层 | `qa_feedback`、`audit_log` | 反馈闭环、审计追踪 |
| 向量检索层 | `knowledge_chunk_embedding` | pgvector 语义检索 |

## 企业级建表要点

- **内部系统不等于不做权限**：虽然不是对外 SaaS，但公司内部仍然需要部门、员工、角色、知识库成员权限。
- **业务唯一性可先交给应用层**：因为当前要求不建唯一索引，所以 `username`、`kb_code`、`document_key`、`chunk_key` 等字段需要在 Service 层做重复校验。
- **状态字段显式建模**：文档、任务、会话都用 `status` 表达生命周期，避免只靠删除判断状态。
- **保留审计字段**：`created_at`、`updated_at` 是生产系统排障、追踪、数据治理的基础。
- **召回来源单独成表**：AI 回答必须能够解释“引用了哪份文档的哪段内容”。
- **索引后置**：先完成业务闭环，再基于慢 SQL、Explain 和压测结果逐步补索引。
