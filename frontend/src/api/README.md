# API 契约（后端需实现）

前端聊天窗口通过 `src/api/chat.js` 调用以下接口。

## 1) 聊天接口

- **Method**: `POST`
- **Path**: `/chat`
- **Content-Type**: `application/json`

### 请求参数（JSON）

```json
{
  "messages": [
    { "role": "user", "content": "什么是 JWT？" }
  ]
}
```

字段说明：

- `messages` (array, 推荐): 当前会话消息流，元素需包含 `role` 与 `content` 字段。
- `message` (string, 可选): 兼容字段。未提供 `messages` 时可仅传最后一条用户输入。

### 响应参数（JSON）

```json
{
  "reply": "JWT 是一种用于安全传输声明信息的令牌..."
}
```

字段说明：

- `reply` (string, 必填): 助手返回的文本结果。

## 2) 错误响应建议

- HTTP `400`：请求参数不合法（例如 message 为空）
- HTTP `500`：服务端内部错误

错误响应建议保持同结构，便于前端统一处理：

```json
{
  "reply": "错误原因说明"
}
```
