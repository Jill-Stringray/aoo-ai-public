-- AOO-AI RBAC 示例数据
-- 覆盖 app_user、role、user_role 三张表，各 5 条示例主数据/关系数据。
--
-- 设计说明：当前建表脚本未在 username、role_code、(user_id, role_id) 上添加唯一约束，
-- 企业级落地时应由后端 Service 层在写入前做存在性与合法性校验，防止重复用户、重复角色、
-- 无效角色绑定等 bad data 进入数据库。

INSERT INTO `app_user` (
  `id`, `department_id`, `username`, `password_hash`, `display_name`,
  `email`, `mobile`, `status`, `last_login_at`, `created_at`, `updated_at`
) VALUES
  (1001, NULL, 'zhangsan', '$2a$10$exampleHashForDemoOnly000000000000000000000000001', '张三', 'zhangsan@example.com', '13800000001', 'ACTIVE', NULL, NOW(3), NOW(3)),
  (1002, NULL, 'lisi', '$2a$10$exampleHashForDemoOnly000000000000000000000000002', '李四', 'lisi@example.com', '13800000002', 'ACTIVE', NULL, NOW(3), NOW(3)),
  (1003, NULL, 'wangwu', '$2a$10$exampleHashForDemoOnly000000000000000000000000003', '王五', 'wangwu@example.com', '13800000003', 'ACTIVE', NULL, NOW(3), NOW(3)),
  (1004, NULL, 'zhaoliu', '$2a$10$exampleHashForDemoOnly000000000000000000000000004', '赵六', 'zhaoliu@example.com', '13800000004', 'LOCKED', NULL, NOW(3), NOW(3)),
  (1005, NULL, 'sunqi', '$2a$10$exampleHashForDemoOnly000000000000000000000000005', '孙七', 'sunqi@example.com', '13800000005', 'ACTIVE', NULL, NOW(3), NOW(3));

INSERT INTO `role` (
  `id`, `role_code`, `role_name`, `description`, `created_at`, `updated_at`
) VALUES
  (2001, 'ADMIN', '系统管理员', '负责系统配置、用户管理和全局治理。', NOW(3), NOW(3)),
  (2002, 'KB_MANAGER', '知识库管理员', '负责知识库创建、成员授权、文档治理。', NOW(3), NOW(3)),
  (2003, 'DOC_EDITOR', '文档编辑员', '负责上传、更新和维护知识文档。', NOW(3), NOW(3)),
  (2004, 'AUDITOR', '审计员', '负责查看审计日志、反馈记录和合规信息。', NOW(3), NOW(3)),
  (2005, 'USER', '普通用户', '使用知识库问答、查看被授权的知识内容。', NOW(3), NOW(3));

INSERT INTO `user_role` (
  `id`, `user_id`, `role_id`, `created_at`
) VALUES
  (3001, 1001, 2001, NOW(3)),
  (3002, 1002, 2002, NOW(3)),
  (3003, 1003, 2003, NOW(3)),
  (3004, 1004, 2004, NOW(3)),
  (3005, 1005, 2005, NOW(3));
