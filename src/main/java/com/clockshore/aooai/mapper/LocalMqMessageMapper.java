package com.clockshore.aooai.mapper;

import com.clockshore.aooai.entity.LocalMqMessage;
import com.clockshore.aooai.enumType.LocalMqMessageStatus;
import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface LocalMqMessageMapper {

    @Insert("""
            INSERT INTO local_mq_message (event_key, topic, routing_key, payload, status, retry_count, next_retry_at)
            VALUES (#{eventKey}, #{topic}, #{routingKey}, #{payload}, #{status}, #{retryCount}, #{nextRetryAt})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(LocalMqMessage message);

    @Select("SELECT COUNT(1) FROM local_mq_message WHERE event_key = #{eventKey}")
    int countByEventKey(@Param("eventKey") String eventKey);

    @Select("""
            SELECT id, event_key AS eventKey, topic, routing_key AS routingKey, payload, status,
                   retry_count AS retryCount, next_retry_at AS nextRetryAt, fail_reason AS failReason,
                   created_at AS createdAt, updated_at AS updatedAt
            FROM local_mq_message
            WHERE status IN ('NEW', 'FAILED')
              AND (next_retry_at IS NULL OR next_retry_at <= NOW(3))
            ORDER BY id
            LIMIT #{limit}
            """)
    List<LocalMqMessage> selectForDispatch(@Param("limit") int limit);

    @Update("""
            UPDATE local_mq_message
            SET status = #{sendingStatus.value},
                fail_reason = NULL
            WHERE id = #{id}
              AND status IN (#{newStatus.value}, #{failedStatus.value})
            """)
    int tryMarkSending(@Param("id") Long id,
                       @Param("sendingStatus") LocalMqMessageStatus sendingStatus,
                       @Param("newStatus") LocalMqMessageStatus newStatus,
                       @Param("failedStatus") LocalMqMessageStatus failedStatus);

    @Update("""
            UPDATE local_mq_message
            SET status = #{sentStatus.value},
                fail_reason = NULL
            WHERE id = #{id}
            """)
    int markSent(@Param("id") Long id, @Param("sentStatus") LocalMqMessageStatus sentStatus);

    @Update("""
            UPDATE local_mq_message
            SET status = #{failedStatus.value},
                retry_count = retry_count + 1,
                next_retry_at = DATE_ADD(NOW(3), INTERVAL #{retryMinutes} MINUTE),
                fail_reason = #{failReason}
            WHERE id = #{id}
            """)
    int markFailed(@Param("id") Long id,
                   @Param("failedStatus") LocalMqMessageStatus failedStatus,
                   @Param("failReason") String failReason,
                   @Param("retryMinutes") int retryMinutes);

    @Update("""
            UPDATE local_mq_message
            SET status = #{failedStatus.value},
                next_retry_at = NOW(3),
                fail_reason = 'SENDING timeout, reset for retry'
            WHERE status = #{sendingStatus.value}
              AND updated_at <= DATE_SUB(NOW(3), INTERVAL #{timeoutMinutes} MINUTE)
            """)
    int recoverTimeoutSending(@Param("failedStatus") LocalMqMessageStatus failedStatus,
                              @Param("sendingStatus") LocalMqMessageStatus sendingStatus,
                              @Param("timeoutMinutes") int timeoutMinutes);
}
