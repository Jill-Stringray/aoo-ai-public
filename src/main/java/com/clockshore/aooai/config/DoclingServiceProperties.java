package com.clockshore.aooai.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "docling.service")
public class DoclingServiceProperties {

    private boolean enabled = true;

    private String baseUrl = "http://localhost:8000";

    private boolean fallbackToLocalReader = true;

    private Duration timeout = Duration.ofMinutes(5);

    private List<String> supportedExtensions = List.of(
            "pdf",
            "docx",
            "doc",
            "pptx",
            "ppt",
            "xlsx",
            "xls",
            "csv",
            "html",
            "htm",
            "md",
            "adoc",
            "asciidoc",
            "odt",
            "ods",
            "odp",
            "epub",
            "eml",
            "msg",
            "xml"
    );
}
