package com.clockshore.aooai.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "rag.text-repair")
public class DocumentTextRepairProperties {

    /**
     * 移除 PDF/OCR 中常见的软连字符。
     */
    private boolean removeSoftHyphen = true;

    /**
     * 修复英文单词在行尾被连字符断开的情况，例如 "exam-\nple"。
     */
    private boolean joinHyphenatedLineBreaks = true;

    /**
     * 将 Unicode 连字符统一成普通半角连字符。
     */
    private boolean normalizeUnicodeHyphens = true;
}
