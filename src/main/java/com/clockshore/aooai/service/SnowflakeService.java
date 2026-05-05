package com.clockshore.aooai.service;

import cn.hutool.core.util.IdUtil;
import org.springframework.stereotype.Service;

@Service
public class SnowflakeService {

    public String nextIdStr() {
        return IdUtil.getSnowflakeNextIdStr();
    }

    public Long nextId(){
        return IdUtil.getSnowflakeNextId();
    }
}