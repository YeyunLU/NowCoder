package com.nowcoder.community.service;

import com.nowcoder.community.dao.LoginTicketMapper;
import com.nowcoder.community.dao.UserMapper;
import com.nowcoder.community.entity.LoginTicket;
import com.nowcoder.community.entity.User;
import com.nowcoder.community.util.CommunityConstant;
import com.nowcoder.community.util.CommunityUtil;
import com.nowcoder.community.util.MailClient;
import com.nowcoder.community.util.RedisKeyUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@Service
public class UserService implements CommunityConstant {
    @Autowired
    private UserMapper userMapper;

//    @Autowired
//    private LoginTicketMapper loginTicketMapper;

    @Autowired
    private MailClient mailClient;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private TemplateEngine templateEngine;

    @Value("${community.path.domain}")
    private String domain;

    @Value("${server.servlet.context-path}")
    private String contextPath;

    public User findUserById(int id){
        User user = getCache(id);
        if(user==null){
            user = initCache(id);
        }
        return user;
//        return userMapper.selectById(id);
    }

    public User findUserByEmail(String email) { return userMapper.selectByEmail(email); }

    public Map<String, Object> register(User user){

        // 空值处理
        Map<String, Object> map = new HashMap<>();
        if(user==null) throw new IllegalArgumentException("参数不能为空");
        if(StringUtils.isBlank(user.getUsername())) {
            map.put("usernameMsg", "账号不能为空！");
            return map;
        }
        if(StringUtils.isBlank(user.getPassword())) {
            map.put("passwordMsg", "密码不能为空！");
            return map;
        }
        if(StringUtils.isBlank(user.getEmail())) {
            map.put("emailMsg", "邮箱不能为空！");
        }

        // 验证账号
        User u = userMapper.selectByName(user.getUsername());
        if(u!=null){
            map.put("usernameMsg", "账号已存在！");
            return map;
        }
        u = userMapper.selectByEmail(user.getEmail());
        if(u!=null){
            map.put("emailMsg", "该邮箱已被注册！");
            return map;
        }

        // 注册用户
        user.setSalt(CommunityUtil.generateUUID().substring(0,5));
        user.setPassword(CommunityUtil.md5(user.getPassword()+user.getSalt()));
        user.setType(0);
        user.setStatus(0);
        user.setActivationCode(CommunityUtil.generateUUID());
        user.setHeaderUrl(String.format(
                "https://images.nowcoder.com/head/%dt.png",
                new Random().nextInt(1000)));
        user.setCreateTime(new Date());
        userMapper.insertUser(user);

        // 发送激活码邮件
        Context context = new Context();
        context.setVariable("email", user.getEmail());
        // http://localhost:8080/community/activation/${userId}/${code}
        String url = domain + contextPath + "/activation/" + user.getId() + "/" + user.getActivationCode();
        context.setVariable("url", url);
        String content = templateEngine.process("/mail/activation", context);
        mailClient.sendMail(user.getEmail(), "激活账号", content);

        return null;
    }

    public int activation(int userId, String activationCode){
        User user = userMapper.selectById(userId);
        if(user.getStatus()==1){ //已被激活
            return ACTIVATION_REPEAT;
        }else if(user.getActivationCode().equals(activationCode)){
            userMapper.updateStatus(userId, 1);
            clearCache(userId);
            return ACTIVATION_SUCCESS;
        }else{
            return ACTIVATION_FAILURE;
        }
    }

    public Map<String, Object> login(String username, String password, int expiredSeconds){
        Map<String, Object> map = new HashMap<>();
        // 空值处理
        if(StringUtils.isBlank(username)){
            map.put("usernameMsg", "账号不能为空！");
            return map;
        }
        if(StringUtils.isBlank(password)){
            map.put("passwordMsg", "密码不能为空！");
            return map;
        }
        // 验证合法性
        User user = userMapper.selectByName(username);
        if(user==null){
            map.put("usernameMsg", "该账号不存在！");
            return map;
        }
        if(user.getStatus()==0){
            map.put("usernameMsg", "该账号未激活！");
            return map;
        }
        password = CommunityUtil.md5(password + user.getSalt());
        if(!user.getPassword().equals(password)){
            map.put("passwordMsg", "密码不正确！");
            return map;
        }

        // 生成登陆凭证
        LoginTicket loginTicket = new LoginTicket();
        loginTicket.setUserId(user.getId());
        loginTicket.setTicket(CommunityUtil.generateUUID());
        loginTicket.setStatus(0);
        loginTicket.setExpired(new Date(System.currentTimeMillis()+ expiredSeconds*1000));

        // 保存凭证
        String redisKey = RedisKeyUtil.getTicketKey(loginTicket.getTicket());
        redisTemplate.opsForValue().set(redisKey, loginTicket); // redis会把对象序列化为JSON字符串
//        loginTicketMapper.insertLoginTicket(loginTicket);

        map.put("ticket", loginTicket.getTicket());

        return map;
    }

    public void logout(String ticket){
        String redisKey = RedisKeyUtil.getTicketKey(ticket);
        LoginTicket loginTicket = (LoginTicket) redisTemplate.opsForValue().get(redisKey);
        loginTicket.setStatus(1);
        redisTemplate.opsForValue().set(redisKey,loginTicket);
//        loginTicketMapper.updateStatus(ticket, 1);
    }

    public Map<String, Object> forgetPassword(String email, String password){
        Map<String, Object> map = new HashMap<>();
        User user = findUserByEmail(email);
        if(user==null){
            map.put("userMsg", "该用户不存在！");
            return map;
        }
        if(password==null || password.length()<8){
            map.put("passwordMsg", "密码长度不能小于8！");
            return map;
        }
        userMapper.updatePassword(user.getId(), CommunityUtil.md5(password+user.getSalt()));
        clearCache(user.getId());
        return map;
    }

    public void sendCode(String email, String code){
        Context context = new Context();
        context.setVariable("email", email);
        context.setVariable("code", code);
        String content = templateEngine.process("/mail/forget", context);
        mailClient.sendMail(email, "忘记密码", content);
    }

    public LoginTicket findLoginTicket(String ticket){
        String redisKey = RedisKeyUtil.getTicketKey(ticket);
        return (LoginTicket) redisTemplate.opsForValue().get(redisKey);
//        return loginTicketMapper.selectByTicket(ticket);
    }

    public int updateHeader(int userId, String headerUrl){
        int rows = userMapper.updateHeader(userId, headerUrl);
        clearCache(userId);
        return rows;
    }

    public int updatePassword(int userId, String password){
        User user = userMapper.selectById(userId);
        clearCache(userId);
        int rows = userMapper.updatePassword(user.getId(),
                CommunityUtil.md5(password+user.getSalt()));
        clearCache(userId);
        return rows;
    }

    public User findUserByName(String username){
        return userMapper.selectByName(username);
    }

    // 1. 优先从缓存取值
    private User getCache(int userId){
        String redisKey = RedisKeyUtil.getUserKey(userId);
        return (User) redisTemplate.opsForValue().get(redisKey);
    }
    // 2. 取不到则初始化缓存
    private User initCache(int userId){
        // 从MySQL读数据
        User user = userMapper.selectById(userId);
        String redisKey = RedisKeyUtil.getUserKey(userId);
        redisTemplate.opsForValue().set(redisKey, user, 3600, TimeUnit.SECONDS);
        return user;
    }
    // 3. 数据变更时清除缓存数据
    private void clearCache(int userId){
        String redisKey = RedisKeyUtil.getUserKey(userId);
        redisTemplate.delete(redisKey);
    }

}
