-- AOO-AI 公司内部向量库建库脚本（PostgreSQL + pgvector）
-- 业务主数据建议放 MySQL（见 aoo-ai.sql），向量检索专用数据放 PostgreSQL。
-- 注意：按当前需求，本脚本不显式创建任何向量索引或普通索引；后续压测确认需要后再补充。

CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE IF NOT EXISTS knowledge_chunk_embedding (
  id BIGSERIAL PRIMARY KEY,
  knowledge_base_id BIGINT NOT NULL,
  document_id BIGINT NOT NULL,
  chunk_id BIGINT NOT NULL,
  chunk_key VARCHAR(128) NOT NULL,
  embedding_model VARCHAR(128) NOT NULL,
  embedding vector(1536) NOT NULL,
  content TEXT NOT NULL,
  metadata JSONB,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
