package com.clockshore.aooai.mq;

import com.clockshore.aooai.enumType.LocalMqMessageStatus;
import com.clockshore.aooai.mapper.LocalMqMessageMapper;
import jakarta.annotation.Resource;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 定期归档已投递的本地 MQ 消息，避免热表长期膨胀影响投递扫描。
 */
@Slf4j
@Component
public class LocalMqMessageArchiveJob {

    @Resource
    private LocalMqMessageMapper localMqMessageMapper;

    @Value("${mq.local-message.archive.enabled:true}")
    private boolean enabled;

    @Value("${mq.local-message.archive.retention-days:1}")
    private int retentionDays;

    @Value("${mq.local-message.archive.batch-size:500}")
    private int batchSize;

    @Scheduled(cron = "${mq.local-message.archive.cron:0 1 2 * * ?}")
    @Transactional(rollbackFor = Exception.class)
    public void archiveSentMessages() {
        if (!enabled) {
            return;
        }

        List<Long> ids = localMqMessageMapper.selectArchiveCandidateIds(
                LocalMqMessageStatus.SENT,
                retentionDays,
                batchSize
        );
        if (ids.isEmpty()) {
            return;
        }

        int archived = localMqMessageMapper.archiveByIds(ids, LocalMqMessageStatus.SENT);
        int deleted = localMqMessageMapper.deleteArchivedByIds(ids, LocalMqMessageStatus.SENT);
        log.info("Local MQ message archive finished, candidates={}, archived={}, deleted={}",
                ids.size(), archived, deleted);
    }
}
