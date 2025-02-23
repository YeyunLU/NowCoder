package com.nowcoder.community.util;

import org.apache.commons.lang3.StringUtils;
import org.springframework.util.DigestUtils;

import java.util.UUID;

public class CommunityUtil {

    // 生成随机激活码
    public static String generateUUID(){
        return UUID.randomUUID().toString().replaceAll("-", "");
    }

    // MD5 加密
    // hello -> abc123def456 简单字符串容易被破解，简单单词表
    // 改进：hello + salt -> abc123def456abc 安全性提高
    public static String md5(String key){
        if(StringUtils.isBlank(key)) return null;
        return DigestUtils.md5DigestAsHex(key.getBytes());
    }

}
