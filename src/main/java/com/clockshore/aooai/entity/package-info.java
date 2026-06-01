/**
 * 数据库实体对象包。
 *
 * <p>本包中的类按照仓库内的 MySQL 建表脚本 {@code aoo-ai.sql}
 * 和 PostgreSQL/pgvector 建表脚本 {@code docs/database/pgvector-schema.sql} 生成，
 * 字段命名采用 Java 常用的 camelCase，与数据库 snake_case 字段通过 MyBatis 下划线转驼峰配置或 ResultMap 对齐。</p>
 */
package com.clockshore.aooai.entity;
