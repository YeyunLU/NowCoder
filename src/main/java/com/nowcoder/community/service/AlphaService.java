package com.nowcoder.community.service;

import com.nowcoder.community.dao.AlphaDao;
import com.nowcoder.community.dao.DiscussPostMapper;
import com.nowcoder.community.dao.UserMapper;
import com.nowcoder.community.entity.DiscussPost;
import com.nowcoder.community.entity.User;
import com.nowcoder.community.util.CommunityUtil;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Date;

@Service
// @Scope("singleton") default
// @Scope("prototype") non-singleton
public class AlphaService {

    @Autowired
    private AlphaDao alphaDao;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private DiscussPostMapper discussPostMapper;

    @Autowired
    private TransactionTemplate transactionTemplate;

    public AlphaService(){
        System.out.println("Construction");
    }

    @PostConstruct
    public void init() {
        System.out.println("Initializing alpha service");
    }

    @PreDestroy
    public void destroy(){
        System.out.println("Destroying alpha service");
    }

    public String find(){
        return alphaDao.select();
    }


    // （主外）REQUIRED：支持外部事务（调用该事务的事务），若不存在则创建新事务
    // （主内）REQUIRED_NEW：创建一个新的事务，并且暂停外部事务
    // （中和）NESTED: 如果存在外部事务，则嵌套执行（子事务有独立提交和回滚），否则和REQUIRED一样（中和）
    @Transactional(isolation = Isolation.READ_COMMITTED, propagation = Propagation.REQUIRED)
    public Object save1(){
        // 新增用户
        User user = new User();
        user.setUsername("alpha");
        user.setSalt(CommunityUtil.generateUUID().substring(0,5));
        user.setPassword(CommunityUtil.md5("123"+user.getSalt()));
        user.setEmail("alpha@qq.com");
        user.setHeaderUrl("http://image/nowcoder/header/99t.png");
        user.setCreateTime(new Date());
        userMapper.insertUser(user);

        // 新增帖子
        DiscussPost discussPost = new DiscussPost();
        discussPost.setUserId(user.getId());
        discussPost.setTitle("hello");
        discussPost.setContent("新人报道！");
        discussPostMapper.insertDiscussPost(discussPost);

        // 制造一个错误，测试回滚
        Integer.valueOf("abc");

        return "ok";
    }

    public Object save2(){
        transactionTemplate.setIsolationLevel(
                TransactionDefinition.ISOLATION_READ_COMMITTED); 
        transactionTemplate.setPropagationBehavior(
                TransactionDefinition.PROPAGATION_REQUIRED);
        return transactionTemplate.execute(
                new TransactionCallback<Object>() {
                    @Override
                    public Object doInTransaction(TransactionStatus status) {
                        // 新增用户
                        User user = new User();
                        user.setUsername("beta");
                        user.setSalt(CommunityUtil.generateUUID().substring(0,5));
                        user.setPassword(CommunityUtil.md5("123"+user.getSalt()));
                        user.setEmail("beta@qq.com");
                        user.setHeaderUrl("http://image/nowcoder/header/999t.png");
                        user.setCreateTime(new Date());
                        userMapper.insertUser(user);

                        // 新增帖子
                        DiscussPost discussPost = new DiscussPost();
                        discussPost.setUserId(user.getId());
                        discussPost.setTitle("你好");
                        discussPost.setContent("我是新人！");
                        discussPostMapper.insertDiscussPost(discussPost);

                        Integer.valueOf("abc");

                        return "ok";
                    }
                }
        );
    }
}
