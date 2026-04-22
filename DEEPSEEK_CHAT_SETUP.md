# 用 DeepSeek 搭建一个简单 AI 对话软件（Spring Boot 版）

这份文档带你从 0 到 1 搭一个“前端页面 + 后端接口 + DeepSeek 模型调用”的最小可用聊天应用。

## 1. 准备工作

- JDK 17+（Spring Boot 3 / Spring AI 要求）
- Maven 3.9+（建议）
- DeepSeek API Key（在 DeepSeek 平台申请）

> 说明：DeepSeek 提供 OpenAI 兼容接口，所以后端以标准 HTTP JSON 调用即可。


## 1.1 IDE / 编译器版本对齐（避免 `source 8` 与 `TypeTag::UNKNOWN`）

如果你看到以下报错：
- `java: 源值 8 已过时`
- `java: 目标值 8 已过时`
- `java.lang.ExceptionInInitializerError`
- `com.sun.tools.javac.code.TypeTag :: UNKNOWN`

请确认：
1. **Project SDK/JDK = 17**（不要让 IDE 回落到 1.8）
2. Maven Reimport 后，`maven-compiler-plugin` 使用 `<release>17</release>`
3. Lombok 插件与依赖版本同步（本项目已升级到 `1.18.38`）

## 2. 项目结构

本项目核心文件如下：

- `src/main/resources/static/index.html`：前端页面
- `src/main/java/com/clockshore/aooai/chat/ChatController.java`：聊天接口
- `src/main/java/com/clockshore/aooai/chat/DeepSeekChatService.java`：调用 DeepSeek
- `src/main/java/com/clockshore/aooai/config/AiProperties.java`：模型配置
- `src/main/resources/application.yaml`：API Key 与模型参数

## 3. 配置 DeepSeek 参数

在 `application.yaml` 中填写：

```yaml
deepseek:
  api-key: YOUR_DEEPSEEK_API_KEY
  base-url: https://api.deepseek.com
  model: deepseek-chat
```

把 `YOUR_DEEPSEEK_API_KEY` 换成你的真实密钥。

## 4. 后端接口设计

### 4.1 请求 / 响应模型

- 请求体：`{ "message": "你好" }`
- 响应体：`{ "reply": "你好呀，有什么我可以帮助你的？" }`

### 4.2 接口路径

- `POST /api/chat`

逻辑：
1. 校验用户输入是否为空。
2. 调用 `DeepSeekChatService`。
3. 把模型回复返回给前端。

## 5. 调用 DeepSeek 的关键点

在 `DeepSeekChatService` 中：

1. 拼接请求地址：`{baseUrl}/chat/completions`
2. Header 带上 `Authorization: Bearer <api-key>`
3. Body 包含：
   - `model`
   - `messages`（最小先传一个 user 消息）
   - `temperature`
4. 解析 `choices[0].message.content` 作为最终文本。

## 6. 前端最小页面

页面包含：
- 文本输入框
- 发送按钮
- 回复展示区域

发送时通过 `fetch('/api/chat')` 调后端，拿到 `reply` 后渲染到页面。

## 7. 运行项目

```bash
mvn spring-boot:run
```

浏览器访问：

- `http://localhost:8080`

## 8. 常见问题

### Q1：提示 401 或鉴权失败

- 检查 `deepseek.api-key` 是否正确。
- 确保 key 没有多余空格。

### Q2：接口调用失败

- 检查网络是否可访问 `https://api.deepseek.com`。
- 查看后端日志里的异常信息。

### Q3：想要多轮对话怎么办

- 现在是“单轮”实现。
- 升级方式：在后端维护 `messages` 历史（可按 session/user 维度存储），每次把历史 + 新问题一起传给模型。

## 9. 下一步可扩展方向

1. 增加上下文记忆（多轮会话）
2. 加入流式输出（SSE / WebSocket）
3. 增加系统提示词（System Prompt）
4. 增加敏感词与安全过滤
5. 增加登录与对话历史保存（MySQL）

---

如果你是第一次做 AI 应用，建议先保持当前最小版本能跑通，再逐步加“多轮 + 流式 + 持久化”。
