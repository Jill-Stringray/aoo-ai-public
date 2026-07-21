package com.clockshore.aooai.mapper;

import com.clockshore.aooai.entity.MessageRetrievalSource;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface MessageRetrievalSourceMapper {


    void batchInsert(
            @Param("list")
            List<MessageRetrievalSource> list
    );

    List<MessageRetrievalSource> selectByMessageKeys(
            @Param("messageKeys")
            List<String> messageKeys
    );

    @Delete("DELETE FROM message_retrieval_source WHERE message_key = #{messageKey}")
    int deleteByMessageKey(@Param("messageKey") String messageKey);

}
