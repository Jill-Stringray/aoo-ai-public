package com.clockshore.aooai;

import com.clockshore.aooai.mapper.AppUserMapper;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.ai.vectorstore.VectorStore;

import java.util.List;

@SpringBootTest
class AooAiApplicationTests {

    @Resource
    private AppUserMapper appUserMapper;
    @Resource
    private BCryptPasswordEncoder bCryptPasswordEncoder;

    @Autowired
    private EmbeddingModel embeddingModel;

    @Autowired
    private VectorStore vectorStore;


    /*@Test
    void test() {
        vectorStore.add(List.of(
                new Document("张三喜欢吃苹果")
        ));
        List<Document> docs =
                vectorStore.similaritySearch("张三喜欢吃什么");
        System.out.println(docs);
    }*/
}
