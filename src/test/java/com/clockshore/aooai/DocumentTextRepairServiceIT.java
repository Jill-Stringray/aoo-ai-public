package com.clockshore.aooai;

import com.clockshore.aooai.config.DocumentTextRepairProperties;
import com.clockshore.aooai.service.DocumentTextRepairService;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class DocumentTextRepairServiceIT {

    private final DocumentTextRepairService repairService =
            new DocumentTextRepairService(new DocumentTextRepairProperties());

    @Test
    void joinsHyphenatedLineBreaksBeforeSplit() {
        String repaired = repairService.repairText("This para contains embed-\nding text and retriev-\nal context.");

        assertThat(repaired).isEqualTo("This para contains embedding text and retrieval context.");
    }

    @Test
    void removesSoftHyphenAndNormalizesUnicodeHyphen() {
        String repaired = repairService.repairText("know\u00ADledge base uses cross\u2011encoder rerank.");

        assertThat(repaired).isEqualTo("knowledge base uses cross-encoder rerank.");
    }

    @Test
    void keepsMetadataWhenRepairingDocument() {
        Map<String, Object> metadata = Map.of(
                "documentId", 10L,
                "pageNo", 2,
                "page_number", 2
        );
        Document repaired = repairService.repair(new Document("embed-\nding", metadata));

        assertThat(repaired.getText()).isEqualTo("embedding");
        assertThat(repaired.getMetadata()).containsAllEntriesOf(metadata);
    }
}
