package com.clockshore.aooai.dto.mq;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.ai.document.Document;

import java.util.Map;

/**
 * 这个类将Spring AI类的document转化成dto，再作为content进行发送MQ，最后转化成RetrievalSource进行持久化存储
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RetrievalSourceDTO {

    /**
     * 文档ID（数据库主键）
     */
    private Long documentId;

    /**
     * 文档唯一Key
     */
    private String documentKey;

    private Long chunkId;

    /**
     * 切片key
     */
    private String chunkKey;

    /**
     * 原始文件名
     */
    private String fileName;

    /**
     * 存储文件名
     */
    private String storedFileName;

    /**
     * 知识库ID
     */
    private Long knowledgeBaseId;

    /**
     * 页码
     */
    private Integer pageNumber;

    /**
     * 当前Chunk序号
     */
    private Integer chunkIndex;

    /**
     * 检索到的文本内容
     */
    private String content;

    /**
     * 相似度
     */
    private Double score;

    public static RetrievalSourceDTO of(Document document){

        Map<String,Object> metadata =
                document.getMetadata();


        return RetrievalSourceDTO.builder()
                .documentId(toLong(metadata.get("documentId")))
                .documentKey(
                        toString(metadata.get("documentKey"))
                )
                .chunkId(toLong(metadata.get("chunkId")))
                .chunkKey(
                        toString(metadata.get("chunkKey"))
                )
                .fileName(
                        toString(metadata.get("fileName"))
                )
                .knowledgeBaseId(toLong(metadata.get("knowledgeBaseId")))
                .pageNumber(
                        toInteger(firstPresent(metadata, "pageNo", "page_number"))
                )
                .content(document.getText())
                .score(document.getScore())
                .build();
    }

    private static Object firstPresent(Map<String, Object> metadata, String... keys) {
        for (String key : keys) {
            Object value = metadata.get(key);
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private static String toString(Object value) {
        return value == null ? null : value.toString();
    }

    private static Long toLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        String text = value.toString();
        return text.isBlank() ? null : Long.valueOf(text);
    }

    private static Integer toInteger(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        String text = value.toString();
        return text.isBlank() ? null : Integer.valueOf(text);
    }
}
