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
Retriever 还未实现
↓
LLM 还未实现
