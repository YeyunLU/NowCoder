package com.nowcoder.community.util;

import com.alibaba.fastjson2.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.DigestUtils;

import java.util.HashMap;
import java.util.Map;
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

    public static String getJsonString(int code, String message, Map<String, Object> map) {
         JSONObject json = new JSONObject();
         json.put("code", code);
         json.put("message", message);
         if(map != null){
             for(String key : map.keySet()){
                 json.put(key,map.get(key));
             }
         }
        return json.toJSONString();
    }

    public static String getJsonString(int code, String message) {
        return getJsonString(code, message, null);
    }

    public static String getJsonString(int code) {
        return getJsonString(code, null, null);
    }

    public static void main(String[] args){
        Map<String, Object> map = new HashMap<>();
        map.put("name","Lucy");
        map.put("age", 29);
        System.out.println(getJsonString(0,"ok", map));
    }

}
