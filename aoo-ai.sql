-- AOO-AI 公司内部企业问答知识库业务库建库脚本（MySQL 5.7+ / 8.x / InnoDB）
-- 设计目标：支撑某公司内部“部门用户 + 知识库 + 文档解析切分 + RAG 问答 + 审计治理”的基础闭环。
-- 注意：按当前需求，本脚本不显式创建任何二级索引、唯一索引或全文索引；仅保留关系型表必需的主键与外键约束。

CREATE DATABASE IF NOT EXISTS `aoo-ai`
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_general_ci;

USE `aoo-ai`;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS `local_mq_message`;
DROP TABLE IF EXISTS `outbox_event`;
DROP TABLE IF EXISTS `audit_log`;
DROP TABLE IF EXISTS `qa_feedback`;
DROP TABLE IF EXISTS `message_retrieval_source`;
DROP TABLE IF EXISTS `conversation_message`;
DROP TABLE IF EXISTS `conversation`;
DROP TABLE IF EXISTS `index_task`;
DROP TABLE IF EXISTS `document_chunk`;
DROP TABLE IF EXISTS `knowledge_document`;
DROP TABLE IF EXISTS `knowledge_base_member`;
DROP TABLE IF EXISTS `knowledge_base`;
DROP TABLE IF EXISTS `user_role`;
DROP TABLE IF EXISTS `role`;
DROP TABLE IF EXISTS `app_user`;
DROP TABLE IF EXISTS `department`;

-- 1. 部门表：公司内部组织结构，用于用户归属、知识权限和审计归属。
CREATE TABLE `department` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '部门ID',
  `parent_id` BIGINT UNSIGNED NULL COMMENT '父部门ID',
  `dept_name` VARCHAR(128) NOT NULL COMMENT '部门名称',
  `dept_path` VARCHAR(512) NOT NULL DEFAULT '/' COMMENT '部门路径，便于应用层构建组织树',
  `sort_no` INT NOT NULL DEFAULT 0 COMMENT '排序号',
  `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_department_parent` FOREIGN KEY (`parent_id`) REFERENCES `department` (`id`) ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='公司内部部门表';

-- 2. 用户表：存储公司员工登录身份与企业画像，密码只保存哈希值。
CREATE TABLE `app_user` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  `department_id` BIGINT UNSIGNED NULL COMMENT '部门ID',
  `username` VARCHAR(64) NOT NULL COMMENT '登录用户名',
  `password_hash` VARCHAR(255) NOT NULL COMMENT 'BCrypt/Argon2 等不可逆密码哈希',
  `display_name` VARCHAR(128) NOT NULL COMMENT '展示姓名',
  `email` VARCHAR(128) NULL COMMENT '公司邮箱',
  `mobile` VARCHAR(32) NULL COMMENT '手机号',
  `status` VARCHAR(32) NOT NULL DEFAULT 'ACTIVE' COMMENT '状态：ACTIVE/LOCKED/DISABLED',
  `last_login_at` DATETIME(3) NULL COMMENT '最后登录时间',
  `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_user_department` FOREIGN KEY (`department_id`) REFERENCES `department` (`id`) ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='公司内部用户表';

-- 3. 角色表：公司内部 RBAC 权限模型的角色定义。
CREATE TABLE `role` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '角色ID',
  `role_code` VARCHAR(64) NOT NULL COMMENT '角色编码，如 ADMIN/KB_MANAGER/USER',
  `role_name` VARCHAR(128) NOT NULL COMMENT '角色名称',
  `description` VARCHAR(512) NULL COMMENT '角色说明',
  `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='公司内部角色表';

-- 4. 用户角色关系表：一个员工可拥有多个内部系统角色。
CREATE TABLE `user_role` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '关系ID',
  `user_id` BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
  `role_id` BIGINT UNSIGNED NOT NULL COMMENT '角色ID',
  `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_user_role_user` FOREIGN KEY (`user_id`) REFERENCES `app_user` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_user_role_role` FOREIGN KEY (`role_id`) REFERENCES `role` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='用户角色关系表';

-- 5. 知识库表：公司内部知识资产的聚合根。
CREATE TABLE `knowledge_base` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '知识库ID',
  `kb_code` VARCHAR(64) NOT NULL COMMENT '知识库编码，建议应用层保证唯一',
  `name` VARCHAR(128) NOT NULL COMMENT '知识库名称',
  `description` VARCHAR(1024) NULL COMMENT '知识库描述',
  `owner_user_id` BIGINT UNSIGNED NULL COMMENT '负责人用户ID',
  `visibility` VARCHAR(32) NOT NULL DEFAULT 'DEPARTMENT' COMMENT '可见性：PRIVATE/DEPARTMENT/COMPANY',
  `status` VARCHAR(32) NOT NULL DEFAULT 'ACTIVE' COMMENT '状态：ACTIVE/ARCHIVED/DISABLED',
  `embedding_model` VARCHAR(128) NULL COMMENT '向量模型名称，便于模型升级追踪',
  `retrieval_config` JSON NULL COMMENT '检索参数，如 topK、相似度阈值、混合检索权重',
  `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_kb_owner` FOREIGN KEY (`owner_user_id`) REFERENCES `app_user` (`id`) ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='公司内部知识库表';

-- 6. 知识库成员表：支持按员工授权访问/管理知识库。
CREATE TABLE `knowledge_base_member` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '成员关系ID',
  `knowledge_base_id` BIGINT UNSIGNED NOT NULL COMMENT '知识库ID',
  `user_id` BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
  `member_role` VARCHAR(32) NOT NULL DEFAULT 'READER' COMMENT '成员角色：OWNER/EDITOR/READER',
  `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_kb_member_kb` FOREIGN KEY (`knowledge_base_id`) REFERENCES `knowledge_base` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_kb_member_user` FOREIGN KEY (`user_id`) REFERENCES `app_user` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='知识库成员表';

-- 7. 文档表：记录公司内部文档、来源、解析状态和对象存储位置。
CREATE TABLE `knowledge_document` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '文档ID',
  `knowledge_base_id` BIGINT UNSIGNED NOT NULL COMMENT '知识库ID',
  `document_key` VARCHAR(128) NOT NULL COMMENT '文档业务唯一键，建议应用层保证唯一',
  `title` VARCHAR(255) NOT NULL COMMENT '文档标题',
  `file_name` VARCHAR(255) NOT NULL COMMENT '原始文件名',
  `file_type` VARCHAR(32) NULL COMMENT '文件类型，如 PDF/DOCX/MD/HTML',
  `mime_type` VARCHAR(128) NULL COMMENT 'MIME 类型',
  `file_size` BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '文件大小，单位字节',
  `storage_url` VARCHAR(1024) NULL COMMENT '对象存储地址或本地存储路径',
  `source_type` VARCHAR(32) NOT NULL DEFAULT 'UPLOAD' COMMENT '来源：UPLOAD/URL/API/MANUAL',
  `source_url` VARCHAR(1024) NULL COMMENT '外部来源地址；内部系统也可能同步内部门户链接',
  `version_no` INT UNSIGNED NOT NULL DEFAULT 1 COMMENT '文档版本号',
  `status` VARCHAR(32) NOT NULL DEFAULT 'UPLOADED' COMMENT '状态：UPLOADED/PARSING/CHUNKING/INDEXING/READY/FAILED/DELETED',
  `checksum` VARCHAR(128) NULL COMMENT '文件摘要，用于去重和版本判断',
  `created_by` BIGINT UNSIGNED NULL COMMENT '创建人',
  `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_document_kb` FOREIGN KEY (`knowledge_base_id`) REFERENCES `knowledge_base` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_document_created_by` FOREIGN KEY (`created_by`) REFERENCES `app_user` (`id`) ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='知识文档表';

-- 8. 文档切片表：保存可被召回的文本块；向量本体建议放 PostgreSQL + pgvector。
CREATE TABLE `document_chunk` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '切片ID',
  `knowledge_base_id` BIGINT UNSIGNED NOT NULL COMMENT '知识库ID',
  `document_id` BIGINT UNSIGNED NOT NULL COMMENT '文档ID',
  `chunk_key` VARCHAR(128) NOT NULL COMMENT '切片业务唯一键，与向量库 metadata 对齐',
  `chunk_index` INT UNSIGNED NOT NULL COMMENT '文档内切片序号',
  `content` MEDIUMTEXT NOT NULL COMMENT '切片文本内容',
  `content_hash` VARCHAR(128) NULL COMMENT '切片内容摘要',
  `token_count` INT UNSIGNED NULL COMMENT 'token 数',
  `page_no` INT UNSIGNED NULL COMMENT '页码',
  `section_title` VARCHAR(255) NULL COMMENT '章节标题',
  `metadata` JSON NULL COMMENT '页码、标题层级、表格信息等扩展元数据',
  `vector_ref` VARCHAR(128) NULL COMMENT 'pgvector 记录ID或外部向量库ID',
  `status` VARCHAR(32) NOT NULL DEFAULT 'NEW' COMMENT '状态：NEW/EMBEDDED/FAILED/DELETED',
  `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_chunk_kb` FOREIGN KEY (`knowledge_base_id`) REFERENCES `knowledge_base` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_chunk_document` FOREIGN KEY (`document_id`) REFERENCES `knowledge_document` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='文档切片表';

-- 9. 索引任务表：解析、切片、向量化等异步任务的状态机。
CREATE TABLE `index_task` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '任务ID',
  `knowledge_base_id` BIGINT UNSIGNED NOT NULL COMMENT '知识库ID',
  `document_id` BIGINT UNSIGNED NOT NULL COMMENT '文档ID',
  `task_type` VARCHAR(32) NOT NULL COMMENT '任务类型：PARSE/CHUNK/EMBED/REINDEX',
  `status` VARCHAR(32) NOT NULL DEFAULT 'PENDING' COMMENT '状态：PENDING/RUNNING/SUCCEEDED/FAILED/CANCELED',
  `retry_count` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '已重试次数',
  `max_retry` INT UNSIGNED NOT NULL DEFAULT 3 COMMENT '最大重试次数',
  `next_retry_at` DATETIME(3) NULL COMMENT '下次重试时间',
  `started_at` DATETIME(3) NULL COMMENT '开始时间',
  `finished_at` DATETIME(3) NULL COMMENT '结束时间',
  `error_message` VARCHAR(2048) NULL COMMENT '失败原因',
  `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_index_task_kb` FOREIGN KEY (`knowledge_base_id`) REFERENCES `knowledge_base` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_index_task_document` FOREIGN KEY (`document_id`) REFERENCES `knowledge_document` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='索引任务表';

-- 10. 会话表：一次员工问答主题的聚合。
CREATE TABLE `conversation` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '会话ID',
  `conversation_key` VARCHAR(128) NOT NULL COMMENT '会话业务唯一键，建议应用层保证唯一',
  `user_id` BIGINT UNSIGNED NULL COMMENT '用户ID',
  `knowledge_base_id` BIGINT UNSIGNED NULL COMMENT '本会话默认知识库',
  `title` VARCHAR(255) NULL COMMENT '会话标题',
  `status` VARCHAR(32) NOT NULL DEFAULT 'ACTIVE' COMMENT '状态：ACTIVE/ARCHIVED/DELETED',
  `is_favorite` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否收藏：0=否，1=是',
  `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_conversation_user` FOREIGN KEY (`user_id`) REFERENCES `app_user` (`id`) ON DELETE SET NULL ON UPDATE CASCADE,
  CONSTRAINT `fk_conversation_kb` FOREIGN KEY (`knowledge_base_id`) REFERENCES `knowledge_base` (`id`) ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='问答会话表';

-- 11. 会话消息表：记录员工问题、AI 回答、系统提示和 token 成本。
CREATE TABLE `conversation_message` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '消息ID',
  `conversation_id` BIGINT UNSIGNED NOT NULL COMMENT '会话ID',
  `message_key` VARCHAR(128) NOT NULL COMMENT '消息业务唯一键，建议应用层保证唯一',
  `parent_message_id` BIGINT UNSIGNED NULL COMMENT '父消息ID，用于多轮追踪',
  `role` VARCHAR(32) NOT NULL COMMENT '角色：SYSTEM/USER/ASSISTANT/TOOL',
  `content` MEDIUMTEXT NOT NULL COMMENT '消息内容',
  `model_name` VARCHAR(128) NULL COMMENT '模型名称',
  `prompt_tokens` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '输入 token',
  `completion_tokens` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '输出 token',
  `total_tokens` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '总 token',
  `latency_ms` INT UNSIGNED NULL COMMENT '响应耗时毫秒',
  `finish_reason` VARCHAR(64) NULL COMMENT '模型结束原因',
  `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_conversation_message_key` (`message_key`),
  CONSTRAINT `fk_message_conversation` FOREIGN KEY (`conversation_id`) REFERENCES `conversation` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_message_parent` FOREIGN KEY (`parent_message_id`) REFERENCES `conversation_message` (`id`) ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='会话消息表';

-- 12. 消息召回来源表：保存回答引用了哪些知识切片，便于可解释与溯源。
CREATE TABLE `message_retrieval_source` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '召回来源ID',
  `message_id` BIGINT UNSIGNED NOT NULL COMMENT 'AI 回答消息ID',
  `knowledge_base_id` BIGINT UNSIGNED NOT NULL COMMENT '知识库ID',
  `document_id` BIGINT UNSIGNED NOT NULL COMMENT '文档ID',
  `chunk_id` BIGINT UNSIGNED NOT NULL COMMENT '切片ID',
  `page_number` INT UNSIGNED NULL COMMENT '引用页码',
  `rank_no` INT UNSIGNED NOT NULL COMMENT '召回排序',
  `score` DECIMAL(10, 6) NULL COMMENT '综合相似度得分',
  `rerank_score` DECIMAL(10, 6) NULL COMMENT '重排得分',
  `quote_text` TEXT NULL COMMENT '回答引用片段快照',
  `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_retrieval_message` FOREIGN KEY (`message_id`) REFERENCES `conversation_message` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_retrieval_kb` FOREIGN KEY (`knowledge_base_id`) REFERENCES `knowledge_base` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_retrieval_document` FOREIGN KEY (`document_id`) REFERENCES `knowledge_document` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_retrieval_chunk` FOREIGN KEY (`chunk_id`) REFERENCES `document_chunk` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='消息召回来源表';

-- 13. 问答反馈表：收集点赞、点踩和纠错，用于内部知识运营改进与后续评测集建设。
CREATE TABLE `qa_feedback` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '反馈ID',
  `message_id` BIGINT UNSIGNED NOT NULL COMMENT '被反馈的回答消息ID',
  `user_id` BIGINT UNSIGNED NULL COMMENT '反馈用户ID',
  `rating` TINYINT NOT NULL COMMENT '评分：1=赞，-1=踩，0=中性',
  `reason` VARCHAR(512) NULL COMMENT '反馈原因',
  `suggestion` TEXT NULL COMMENT '员工建议答案或补充说明',
  `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_feedback_message` FOREIGN KEY (`message_id`) REFERENCES `conversation_message` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_feedback_user` FOREIGN KEY (`user_id`) REFERENCES `app_user` (`id`) ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='问答反馈表';

-- 14. 审计日志表：记录登录、知识库变更、文档操作、敏感问答等内部系统行为。
CREATE TABLE `audit_log` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '审计ID',
  `user_id` BIGINT UNSIGNED NULL COMMENT '操作者用户ID',
  `action` VARCHAR(128) NOT NULL COMMENT '动作，如 KB_CREATE/DOC_UPLOAD/QA_ASK',
  `resource_type` VARCHAR(64) NOT NULL COMMENT '资源类型',
  `resource_id` VARCHAR(128) NULL COMMENT '资源ID',
  `request_id` VARCHAR(128) NULL COMMENT '请求链路ID',
  `ip_address` VARCHAR(64) NULL COMMENT '公司内网客户端 IP',
  `user_agent` VARCHAR(512) NULL COMMENT '客户端 UA',
  `detail` JSON NULL COMMENT '审计详情',
  `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_audit_user` FOREIGN KEY (`user_id`) REFERENCES `app_user` (`id`) ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='审计日志表';


-- 16. 本地 MQ 消息表：业务事务先写本地消息，再由后台任务可靠投递到 RabbitMQ。
CREATE TABLE `local_mq_message` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '消息ID',
  `event_key` VARCHAR(128) NOT NULL COMMENT '事件唯一键，建议应用层保证幂等',
  `topic` VARCHAR(128) NOT NULL COMMENT 'MQ 交换机或主题名称',
  `routing_key` VARCHAR(128) NOT NULL COMMENT 'MQ 路由键',
  `payload` JSON NOT NULL COMMENT '消息载荷',
  `status` VARCHAR(32) NOT NULL DEFAULT 'NEW' COMMENT '状态：NEW/SENDING/SENT/FAILED',
  `retry_count` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '已重试次数',
  `next_retry_at` DATETIME(3) NULL COMMENT '下次重试时间',
  `fail_reason` VARCHAR(2048) NULL COMMENT '失败原因',
  `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_local_mq_message_event_key` (`event_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='本地 MQ 消息表';

SET FOREIGN_KEY_CHECKS = 1;
