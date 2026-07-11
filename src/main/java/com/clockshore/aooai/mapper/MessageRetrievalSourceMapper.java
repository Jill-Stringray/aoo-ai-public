package com.clockshore.aooai.mapper;

import com.clockshore.aooai.entity.MessageRetrievalSource;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface MessageRetrievalSourceMapper {


    void batchInsert(
            @Param("list")
            List<MessageRetrievalSource> list
    );

}