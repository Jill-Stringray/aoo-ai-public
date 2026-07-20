package com.clockshore.aooai;

import com.clockshore.aooai.mapper.KnowledgeDocumentMapper;
import com.clockshore.aooai.service.DocumentReaderService;
import com.clockshore.aooai.service.KnowledgeService;
import com.clockshore.aooai.service.TokenSplitterService;
import dev.langchain4j.data.segment.TextSegment;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.IntSummaryStatistics;
import java.util.List;

@SpringBootTest(classes = AooAiApplication.class)
class UploadIT {


    @Autowired
    private KnowledgeService knowledgeService;

    @Autowired
    private DocumentReaderService documentReaderService;

    @Autowired
    private TokenSplitterService tokenSplitterService;

    @Autowired
    private KnowledgeDocumentMapper knowledgeDocumentMapper;



    @Test
    void testReadAndSplit() throws Exception {

        //此测试不走mysql入表
        MockMultipartFile file =
                new MockMultipartFile(
                        "file",
                        "demo-knowledge.pdf",
                        "application/pdf",
                        Files.readAllBytes(
                                Paths.get(
                                        "src/main/resources/demo-knowledge.pdf"
                                )
                        )
                );

        Long fileId =
                knowledgeService.upload(file);

        List<Document> docs =
                documentReaderService.read(
                        fileId
                );

        System.out.println(
                "Document数量：" +
                        docs.size()
        );

        docs = documentReaderService.normalize(docs);

        List<TextSegment> chunks = tokenSplitterService.split(docs);



        System.out.println(
                "Chunk数量：" +
                        chunks.size()
        );

        chunks.stream()
                .limit(chunks.size())
                .forEach(chunk -> {

                    System.out.println(
                            "=========="
                    );

                    System.out.println(chunk);
                    System.out.println("该 chunk 内容长度为：" + chunk.text().length());
                });

        IntSummaryStatistics stats =
                chunks.stream()
                        .mapToInt(
                                chunk -> chunk.text().length()
                        )
                        .summaryStatistics();

        System.out.println("\n统计结果：");
        System.out.println("Min=" + stats.getMin());
        System.out.println("Max=" + stats.getMax());
        System.out.println("Avg=" + stats.getAverage());
        System.out.println("Count=" + stats.getCount());
    }
}