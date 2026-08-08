# 企业级 AI 知识库问答平台 相关设计内容备忘录

## RAG流程 包含Spring AI 和Langchain4j

文件
↓
Reader Spring AI
↓
Document Spring AI
↓
Splitter Langchain4j -> Spring ai的Document 类型 转化为 Langchain4J 的TextSegment 类型（注：若使用spring ai 的spliter得到的类型依然是Document，因为Langchain设计时认为文档和切片不是一个概念，与Spring ai设计思路相反）
↓
Embedding Spring AI
↓
VectorStore Spring AI
↓
Retriever Spring AI
↓
LLM Deepseek

REST API 一般更倾向于返回对象而不是裸值
不要相信前端传的字段数据
框架对象不要跨层传播，数据库实体不要作为消息协议，跨模块传输应该使用 DTO/Event。
例如Document 属于 AI 框架（临时对象），Entity 属于数据库（持久对象），DTO/Event 属于消息通信（传输对象）。三者数据可以相同，但职责不同，不要因为字段一样就混用
