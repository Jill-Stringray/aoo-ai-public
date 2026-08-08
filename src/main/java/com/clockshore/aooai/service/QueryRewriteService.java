package com.clockshore.aooai.service;

import com.clockshore.aooai.Utility.JsonUtil;
import com.clockshore.aooai.entity.QueryRewriteResult;
import com.clockshore.aooai.enumType.Prompt;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class QueryRewriteService {

    private static final String CACHE_KEY_PREFIX = "query:rewrite:v1:";
    private static final long CACHE_TTL_HOURS = 24L;

    private final ChatClient.Builder chatClientBuilder;

    private final JsonUtil jsonUtil;

    private final StringRedisTemplate stringRedisTemplate;

    public QueryRewriteResult rewrite(String query){
        long startedAt = System.nanoTime();
        // 使用原始 query 的 MD5 作为缓存后缀，避免长文本或特殊字符直接进入 Redis key。
        String cacheKey = CACHE_KEY_PREFIX + md5(query);

        // 命中缓存时直接返回解析后的改写结果，避免重复调用大模型。
        String cachedResult = stringRedisTemplate.opsForValue().get(cacheKey);
        if (cachedResult != null) {
            log.info("query rewrite cache hit, key={}", cacheKey);
            QueryRewriteResult queryRewriteResult = jsonUtil.fromJson(cachedResult, new TypeReference<QueryRewriteResult>() {});
            log.info("query rewrite cost={}ms", elapsedMs(startedAt));
            return queryRewriteResult;
        }

        log.info("query rewrite cache miss, key={}", cacheKey);

        String result = chatClientBuilder.build()
                .prompt()
                .system(Prompt.USER_QUERY_REWRITE.getPrompt())
                .user(query)
                .call()
                .content();


        QueryRewriteResult queryRewriteResult = jsonUtil.fromJson(result, new TypeReference<QueryRewriteResult>() {});
        // 仅缓存已经成功解析的结构化结果，过期时间保持 24 小时。
        stringRedisTemplate.opsForValue().set(cacheKey, jsonUtil.toJson(queryRewriteResult), CACHE_TTL_HOURS, TimeUnit.HOURS);
        log.info("query rewrite cost={}ms", elapsedMs(startedAt));
        return queryRewriteResult;
    }

    private String md5(String value) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("MD5");
            byte[] digest = messageDigest.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("MD5 algorithm not available", e);
        }
    }

    private long elapsedMs(long startedAt) {
        return (System.nanoTime() - startedAt) / 1_000_000;
    }
}
