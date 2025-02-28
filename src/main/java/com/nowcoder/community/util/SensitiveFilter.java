package com.nowcoder.community.util;

import jakarta.annotation.PostConstruct;
import org.apache.commons.lang3.CharUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

@Component
public class SensitiveFilter {

    private static final Logger logger = LoggerFactory.getLogger(SensitiveFilter.class);

    // 替换符
    private static final String REPLACEMENT = "***";

    // 跟节点
    private final TrieNode root = new TrieNode();

    @PostConstruct
    public void init(){
        // 写在try(...)里保证finally自动关闭清理
        try(
                InputStream is = this.getClass().getClassLoader().getResourceAsStream("sensitive-words.txt");
                BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                ){
            String keyword;
            while((keyword=reader.readLine())!=null){
                //添加到前缀树
                this.addKeyword(keyword);
            }
        }catch (IOException e){
            logger.error("加载敏感词文件失败！" + e);
        }

    }

    /**
     * 将一个敏感词添加到前缀树
     */
    public void addKeyword(String keyword){
        TrieNode current = root;
        for (char c : keyword.toCharArray()){
            TrieNode subNode = current.getSubNode(c);
            if(subNode==null){
                // 初始化子节点
                subNode = new TrieNode();
                current.addSubNode(c, subNode);
            }
            // 移动指针指向下一个节点
            current = subNode;
        }
        // 设置终止符
        current.isEnd = true;
    }

    /**
     * 检索并过滤敏感词
     *
     * @param text 待过滤文本
     * @return 过滤后的文本
     */
    public String filter(String text){
        if(StringUtils.isBlank(text)) return null;
        TrieNode curr = root;
        int begin = 0, end = 0;
        StringBuilder sb = new StringBuilder();
        char[] chars = text.toCharArray();
        while(end<chars.length){
            char c = chars[end];
            // 跳过符号
            if (isSymbol(c)) {
                if (curr == root) {
                    sb.append(c);
                    begin++;
                }
                end++;
                continue;
            }
            // 检查下级节点
            curr = curr.getSubNode(c);
            if(curr==null){
                // 以begin开头的不是敏感词
                sb.append(chars[begin]);
                end = ++begin;
                // 重新指向跟节点
                curr = root;
            }else if(curr.isEnd()){
                // 从begin到end是敏感词
                sb.append(REPLACEMENT);
                begin = ++end;
                // 重新指向跟节点
                curr = root;
            }else{
                // 检查下一个字符
                end++;
            }
        }
        return sb.toString();
    }

    // 判断是否为符号
    private boolean isSymbol(Character c){
        // 0x2E80 ~ 0x9FFF 为东亚文字范围
        return !CharUtils.isAsciiAlphanumeric(c) && (c<0x2E80 || c>0x9FFF);
    }

    // 前缀树
    private class TrieNode {
        private boolean isEnd = false;
        // 子节点（key是下级节点字符，value是下级节点）
        private Map<Character, TrieNode> subNodes = new HashMap<>();

        public boolean isEnd() {
            return isEnd;
        }

        public void setEnd(boolean end) {
            isEnd = end;
        }

        public void addSubNode(Character c, TrieNode node){
            subNodes.put(c, node);
        }

        public TrieNode getSubNode(Character c){
            return subNodes.get(c);
        }
    }
}
