package com.clockshore.aooai.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "rag.chunk-quality")
public class ChunkQualityProperties {

    /**
     * 低于该长度的 chunk 直接过滤。
     */
    private int minTextLength = 50;

    /**
     * 非空行中单字符行占比上限。
     */
    private double maxSingleCharLineRatio = 0.35;

    /**
     * 换行符数量 / 文本长度的上限。
     */
    private double maxNewlineRatio = 0.12;

    /**
     * 非空行中疑似连续单词拆分行占比上限。
     */
    private double maxSplitWordLineRatio = 0.6;

    /**
     * 参与连续单词拆分检测的最小非空行数。
     */
    private int minSplitWordLineCount = 4;

    /**
     * 非空白字符中有效字母/数字占比下限。
     */
    private double minAlphaNumericRatio = 0.35;

    /**
     * 非空白字符中最高频重复字符占比上限。
     */
    private double maxRepeatedCharRatio = 0.35;

    /**
     * 单个字符连续重复次数上限。
     */
    private int maxRepeatedCharRunLength = 8;
}
