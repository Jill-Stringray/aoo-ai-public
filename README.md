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
- 可用的 硅基流动 API Key，主要是为了方便使用Embedding，Reranker

### 2. 初始化数据库

执行根目录的 `aoo-ai.sql`，并根据 `docs/database/pgvector-schema.sql` 初始化 PostgreSQL 向量库。

### 3. 配置服务

在 `src/main/resources/application.yml` 中配置数据库、Redis、消息队列、Nacos 与模型服务连接信息。

启动前在本机环境中设置以下变量。数据库地址默认指向本机，其他连接参数可在配置文件中调整。

| 环境变量 | 用途 |
| --- | --- |
| `MYSQL_USERNAME` 和 `MYSQL_PASSWORD` | MySQL 连接凭据 |
| `POSTGRES_USERNAME` 和 `POSTGRES_PASSWORD` | PostgreSQL 向量库连接凭据 |
| `DEEPSEEK_API_KEY` 和 `SILICONFLOW_API_KEY` | 对话与向量服务的 API Key |
| `JWT_SECRET` | 用于签发 JWT 的随机密钥，至少使用 32 字节 |
| `MCP_ENABLED` 和 `MCP_TAVILY_URL` | 可选的 MCP 搜索服务，默认关闭 |
| `XXL_JOB_ACCESS_TOKEN` | 启用 XXL-JOB 后按调度中心的配置设置 |

`.env.example` 列出了常用的变量名。请通过系统环境变量或 IDE 的运行配置传入实际值。

### 4. 启动后端

```bash
mvn spring-boot:run
```

默认服务端口为 `8080`。

### 5. 启动Gateway

Gateway可在另外一个仓库：“aoo-ai gateway”中找到。

### 5. 启动前端

```bash
cd frontend
npm install
npm run dev
```

启动后根据 Vite 控制台输出的地址访问前端。
