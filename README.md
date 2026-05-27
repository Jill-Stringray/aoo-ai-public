# aoo-ai 企业级知识库问答平台（工程化脚手架）

本仓库已重构为“可继续扩展”的企业级 AI 知识库问答平台基础工程，当前重点是：

- 清晰的前后端模块边界
- 可演进的 RAG 工作流接口
- 可落地的企业级能力占位（权限、任务、监控、多租户）

## 后端（Spring Boot 3 / Java 17）

已提供：
- 启动入口与配置分层（`application.yml` + `application-local.yml`）
- 文档状态枚举与问答 DTO
- 问答接口：普通回答 + SSE 流式占位
- 文档接口：上传与状态查询占位

## 前端（Vue3 + Vite）

已提供：
- 导航型信息架构页面
- 问答页面与后端接口联调示例
- 管理台、知识库页面占位

## 下一步建议

1. 接入真实文件上传与存储（MinIO/OSS/S3）
2. 增加异步任务系统（RabbitMQ + Worker）
3. 打通 PostgreSQL + pgvector + BM25 + Rerank
4. 接入 JWT、RBAC、多租户隔离
5. 增加可观测性（Micrometer + Prometheus + Grafana）
