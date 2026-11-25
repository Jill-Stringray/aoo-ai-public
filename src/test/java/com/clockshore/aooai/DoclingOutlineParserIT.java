package com.clockshore.aooai;

import com.clockshore.aooai.Utility.JsonUtil;
import com.clockshore.aooai.config.DoclingServiceProperties;
import com.clockshore.aooai.service.DocumentReaderService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.web.reactive.function.client.WebClient;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DoclingOutlineParserIT {

    private static final Path OUTLINE_JSON = Path.of("src/test/resources/docling/demo-outline.json");

    @Test
    void parseOutlineJsonToSectionDocumentsWithPdfPathMetadata() throws Exception {
        assertThat(Files.exists(OUTLINE_JSON)).isTrue();

        DocumentReaderService service = new DocumentReaderService(
                null,
                new DoclingServiceProperties(),
                WebClient.builder().build(),
                createJsonUtil()
        );

        List<Document> documents = service.readDoclingOutline(
                OUTLINE_JSON,
                "demo-knowledge.pdf"
        );

        for(Document d : documents){
            System.out.println("\n\n*******执行getText*******\n\n");
            System.out.println(d.getText());
            System.out.println("\n\n*******执行getMetadata*******\n\n");
            System.out.println(d.getMetadata());
        }

        assertThat(documents).isNotEmpty();
        assertThat(documents)
                .allSatisfy(document -> {
                    assertThat(document.getText()).isNotBlank();
                    assertThat(document.getMetadata()).containsKeys(
                            "source",
                            "fileName",
                            "sectionTitle",
                            "sectionPath",
                            "page_number",
                            "pageNo"
                    );
                });

        assertThat(documents)
                .anySatisfy(document -> {
                    assertThat(document.getMetadata())
                            .containsEntry("sectionTitle", "Service Hours")
                            .containsEntry(
                                    "sectionPath",
                                    List.of("Demo Knowledge Base", "Service Hours")
                            )
                            .containsEntry("page_number", 1);
                    assertThat(document.getText()).contains("Monday through Friday");
                });
    }

    private JsonUtil createJsonUtil() throws Exception {
        JsonUtil jsonUtil = new JsonUtil();
        Field objectMapper = JsonUtil.class.getDeclaredField("objectMapper");
        objectMapper.setAccessible(true);
        objectMapper.set(jsonUtil, new ObjectMapper());
        return jsonUtil;
    }
}
