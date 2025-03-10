package com.nowcoder.community.controller;

import com.nowcoder.community.entity.Comment;
import com.nowcoder.community.entity.DiscussPost;
import com.nowcoder.community.entity.Page;
import com.nowcoder.community.entity.User;
import com.nowcoder.community.service.CommentService;
import com.nowcoder.community.service.DiscussPostService;
import com.nowcoder.community.service.LikeService;
import com.nowcoder.community.service.UserService;
import com.nowcoder.community.util.CommunityConstant;
import com.nowcoder.community.util.CommunityUtil;
import com.nowcoder.community.util.HostHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.*;

@Controller
@RequestMapping("/discuss")
public class DiscussPostController implements CommunityConstant {

    @Autowired
    DiscussPostService discussPostService;

    @Autowired
    UserService userService;

    @Autowired
    CommentService commentService;

    @Autowired
    LikeService likeService;

    @Autowired
    HostHolder hostHolder;

    @RequestMapping(path="/add", method = RequestMethod.POST)
    @ResponseBody
    public String addDiscussPost(String title, String content){
        User user = hostHolder.getUser();
        if(user==null){
            return CommunityUtil.getJsonString(403, "你还没有登录！");
        }
        DiscussPost post = new DiscussPost();
        post.setUserId(user.getId());
        post.setTitle(title);
        post.setContent(content);
        post.setCreateTime(new Date());
        discussPostService.addDiscussPost(post);
        // TODO：处理报错
        return CommunityUtil.getJsonString(0,"发布成功！");
    }

    @RequestMapping(path="/details/{discussPostId}", method = RequestMethod.GET)
    public String getDiscussPost(@PathVariable int discussPostId, Model model, Page page){
        // 帖子
        DiscussPost post = discussPostService.findDiscussPostById(discussPostId);
        model.addAttribute("post", post);
        // 作者
        User user = userService.findUserById(post.getUserId());
        model.addAttribute("user", user);
        // 点赞数量
        long likeCount = likeService.findEntityLikeCount(ENTITY_TYPE_POST, post.getId());
        model.addAttribute("likeCount", likeCount);
        // 点赞状态
        int likeStatus = hostHolder.getUser()==null? 0 :
                likeService.findEntityLikeStatus(
                        hostHolder.getUser().getId(),
                        ENTITY_TYPE_POST, post.getId());
        model.addAttribute("likeStatus", likeStatus);
        // 查评论的分页信息
        page.setLimit(5);
        page.setPath("/discuss/details/" + discussPostId);
        page.setRows(post.getCommentCount());

        // 评论：给帖子的评论
        // 回复：给评论的评论

        // 评论列表
        List<Comment> commentList = commentService.findCommentsByEntityType(ENTITY_TYPE_POST, post.getId(),
                page.getOffset(), page.getLimit());
        // 评论View Object列表
        List<Map<String, Object>> commentViewObjectList = new ArrayList<>();
        if(commentList!=null){
            for(Comment comment : commentList){
                // 评论VO
                Map<String, Object> commentVo = new HashMap<>();
                // 评论
                commentVo.put("comment", comment);
                // 作者
                commentVo.put("user", userService.findUserById(comment.getUserId()));
                // 点赞数量
                likeCount = likeService.findEntityLikeCount(ENTITY_TYPE_COMMENT, comment.getId());
                commentVo.put("likeCount", likeCount);
                // 点赞状态
                likeStatus = hostHolder.getUser()==null? 0 :
                        likeService.findEntityLikeStatus(
                                hostHolder.getUser().getId(),
                                ENTITY_TYPE_COMMENT, comment.getId());
                commentVo.put("likeStatus", likeStatus);

                // 回复列表
                List<Comment> replyList = commentService.findCommentsByEntityType(ENTITY_TYPE_COMMENT, comment.getId(),
                        0, Integer.MAX_VALUE); // 有多少查多少
                // 回复的VO列表
                List<Map<String, Object>> replyViewObjectList = new ArrayList<>();
                if(replyList!=null){
                    for(Comment reply : replyList){
                        // 回复VO
                        Map<String, Object> replyVo = new HashMap<>();
                        // 回复
                        replyVo.put("reply", reply);
                        // 作者
                        replyVo.put("user", userService.findUserById(reply.getUserId()));
                        // 回复目标
                        User target = reply.getTargetId()==0? null : userService.findUserById(reply.getTargetId());
                        replyVo.put("target", target);
                        // 点赞数量
                        likeCount = likeService.findEntityLikeCount(ENTITY_TYPE_COMMENT, reply.getId());
                        replyVo.put("likeCount", likeCount);
                        // 点赞状态
                        likeStatus = hostHolder.getUser()==null? 0 :
                                likeService.findEntityLikeStatus(
                                        hostHolder.getUser().getId(),
                                        ENTITY_TYPE_COMMENT, reply.getId());
                        replyVo.put("likeStatus", likeStatus);

                        replyViewObjectList.add(replyVo);
                    }
                }
                commentVo.put("replies", replyViewObjectList);
                // 回复数量
                int replyCount = commentService.findCommentCount(ENTITY_TYPE_COMMENT, comment.getId());
                commentVo.put("replyCount", replyCount);

                commentViewObjectList.add(commentVo);
            }
        }

        model.addAttribute("comments", commentViewObjectList);

        return "/site/discuss-detail";
    }

}
