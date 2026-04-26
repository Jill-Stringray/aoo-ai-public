# Frontend (Vue 3 + Vite)

这是一个用于测试后端聊天接口的前端对话窗口。

## 启动

```bash
cd frontend
npm install
npm run dev
```

打开 `http://localhost:5173`。

## 目录说明

- `src/App.vue`: 对话窗口页面（消息列表 + 输入框 + 发送按钮）。
- `src/api/chat.js`: 聊天接口调用封装。
- `src/api/README.md`: 提供给后端同学的接口契约（入参/出参/错误约定）。

## 当前前端依赖的后端接口

- `POST /chat`
- Request JSON: `{"message":"你的问题"}`
- Response JSON: `{"reply":"模型回复"}`
