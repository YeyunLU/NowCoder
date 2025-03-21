package com.nowcoder.community.service;

import com.nowcoder.community.dao.CommentMapper;
import com.nowcoder.community.entity.Comment;
import com.nowcoder.community.util.CommunityConstant;
import com.nowcoder.community.util.SensitiveFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.HtmlUtils;

import java.util.List;

@Service
public class CommentService implements CommunityConstant {

    @Autowired
    private CommentMapper commentMapper;

    @Autowired
    private DiscussPostService discussPostService;

    @Autowired
    private SensitiveFilter sensitiveFilter;

    public List<Comment> findCommentsByEntityType(int entityType, int entityId, int offset, int limit) {
        return commentMapper.selectCommentByEntity(entityType,entityId,offset,limit);
    }

    public List<Comment> findCommentsByUser(int userId, int entityType, int offset, int limit){
        return commentMapper.selectCommentByUser(userId, entityType, offset, limit);
    }

    public Comment findCommentById(int id){
        return commentMapper.selectCommentById(id);
    }

    public int findCommentCount(int entityType, int entityId){
        return commentMapper.selectCountByEntity(entityType, entityId);
    }

    public int findCommentCountByUser(int entityType, int userId){
        return commentMapper.selectCountByUser(entityType, userId);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED, propagation = Propagation.REQUIRED)
    public int addComment(Comment comment){
        if(comment==null){
            throw new IllegalArgumentException("参数不能为空！");
        }
        // 过滤标签
        comment.setContent(HtmlUtils.htmlEscape(comment.getContent()));
        // 过滤敏感词
        comment.setContent(sensitiveFilter.filter(comment.getContent()));
        // 添加评论
        int rows = commentMapper.insertComment(comment);
        // 增加评论数量
        if(comment.getEntityType()==ENTITY_TYPE_POST){
            int count = commentMapper.selectCountByEntity(comment.getEntityType(), comment.getEntityId());
            discussPostService.updateCommentCount(comment.getEntityId(), count);
        }
        return rows;
    }
}
