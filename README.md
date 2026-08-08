# aoo-ai

一个基于 RAG（检索增强生成）的企业知识库问答平台。用户可以上传文档、构建知识库，并通过 AI 对话获取带检索上下文的回答。

## 功能

- 用户登录与 JWT 鉴权
- 知识库与文档管理
- PDF、Office 等文档解析与切片
- 基于 pgvector 的向量检索
- DeepSeek/OpenAI 兼容模型对话
- SSE 流式回答与引用来源展示
- RAG 调试、会话管理与后台管理

## 技术栈

**后端**

- Java 17、Spring Boot 3、Spring AI
- MyBatis、MySQL、PostgreSQL + pgvector
- Redis、RabbitMQ、Nacos
- LangChain4j

**前端**

- Vue 3、Vite、Vue Router

## 项目结构

```text
aoo-ai/
├── src/             # Spring Boot 服务
├── frontend/        # Vue 3 前端
├── gateway/         # 网关服务
├── docs/            # 设计与数据库文档
└── aoo-ai.sql       # MySQL 初始化脚本
```

## 快速开始

### 1. 准备环境

- JDK 17+
- Maven 3.9+
- Node.js 18+
- MySQL、PostgreSQL（安装 pgvector）、Redis、RabbitMQ、Nacos
- 可用的 OpenAI 兼容模型 API Key，例如 DeepSeek

### 2. 初始化数据库

执行根目录的 `aoo-ai.sql`，并根据 `docs/database/pgvector-schema.sql` 初始化 PostgreSQL 向量库。

### 3. 配置服务

在 `src/main/resources/application.yml` 中配置数据库、Redis、消息队列、Nacos 与模型服务连接信息。

请使用自己的 API Key 和本地密码，避免将真实凭据提交到 Git 仓库。

### 4. 启动后端

```bash
mvn spring-boot:run
```

默认服务端口为 `8081`。

### 5. 启动前端

```bash
cd frontend
npm install
npm run dev
```

启动后根据 Vite 控制台输出的地址访问前端。
