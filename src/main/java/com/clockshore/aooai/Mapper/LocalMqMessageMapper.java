package com.clockshore.aooai.Mapper;

import com.clockshore.aooai.Entity.LocalMqMessage;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface LocalMqMessageMapper {

    @Insert("""
            INSERT INTO local_mq_message (topic, routing_key, payload, status, retry_count, next_retry_at)
            VALUES (#{topic}, #{routingKey}, #{payload}, #{status}, #{retryCount}, #{nextRetryAt})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(LocalMqMessage message);

    @Select("""
            SELECT id, topic, routing_key AS routingKey, payload, status, retry_count AS retryCount,
                   next_retry_at AS nextRetryAt, fail_reason AS failReason, created_at AS createdAt, updated_at AS updatedAt
            FROM local_mq_message
            WHERE status IN ('NEW', 'FAILED')
              AND (next_retry_at IS NULL OR next_retry_at <= NOW())
            ORDER BY id ASC
            LIMIT #{limit}
            """)
    List<LocalMqMessage> selectForDispatch(@Param("limit") int limit);

    @Update("""
            UPDATE local_mq_message
            SET status = 'SENT',
                fail_reason = NULL
            WHERE id = #{id}
            """)
    int markSent(@Param("id") Long id);

    @Update("""
            UPDATE local_mq_message
            SET status = 'FAILED',
                retry_count = retry_count + 1,
                next_retry_at = DATE_ADD(NOW(), INTERVAL #{retryMinutes} MINUTE),
                fail_reason = #{failReason}
            WHERE id = #{id}
            """)
    int markFailed(@Param("id") Long id, @Param("failReason") String failReason, @Param("retryMinutes") int retryMinutes);
}
