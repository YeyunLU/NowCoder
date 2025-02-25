package com.nowcoder.community.controller;

import com.google.code.kaptcha.Producer;
import com.nowcoder.community.entity.User;
import com.nowcoder.community.service.UserService;
import com.nowcoder.community.util.CommunityConstant;
import com.nowcoder.community.util.CommunityUtil;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

@Controller
public class LoginController implements CommunityConstant {

    @Autowired
    private UserService userService;

    @Autowired
    private Producer kaptchaProducer;

    @Value("${server.servlet.context-path}")
    private String contextPath;

    private static final Logger logger = LoggerFactory.getLogger(LoginController.class);

    @RequestMapping(path="/register", method = RequestMethod.GET)
    public String getRegisterPage(){
        return "/site/register";
    }

    @RequestMapping(path="/login", method = RequestMethod.GET)
    public String getLoginPage(){
        return "/site/login";
    }

    @RequestMapping(path="/forget", method=RequestMethod.GET)
    public String getForgetPasswordPage() {return "/site/forget"; }

    @RequestMapping(path="/register", method = RequestMethod.POST)
    public String register(Model model, User user){
        Map<String, Object> map = userService.register(user);
        // 注册成功，回到首页，注册失败，留在原点
        if (map==null || map.isEmpty()){
            model.addAttribute("msg", "注册成功，我们已经向您的邮箱发送了一封激活邮件，请尽快激活！");
            model.addAttribute("target", "/index");
            return "/site/operate-result";
        } else {
            model.addAttribute("usernameMsg", map.get("usernameMsg"));
            model.addAttribute("passwordMsg", map.get("passwordMsg"));
            model.addAttribute("emailMsg", map.get("emailMsg"));
            return "/site/register";
        }
    }

    // http://localhost:8080/community/activation/${userId}/${code}
    @RequestMapping(path = "/activation/{userId}/{code}", method = RequestMethod.GET)
    public String activation(Model model, @PathVariable("userId") int userId, @PathVariable("code") String code){
        int result = userService.activation(userId, code);
        if(result==ACTIVATION_SUCCESS){
            model.addAttribute("msg", "激活成功，您的账号已经可以正常使用了！");
            model.addAttribute("target", "/login");
        }else if(result==ACTIVATION_REPEAT){
            model.addAttribute("msg", "无效操作，您的账号已经激活过了！");
            model.addAttribute("target", "/index");
        }else{
            model.addAttribute("msg", "激活失败，您的激活码不正确！");
            model.addAttribute("target", "/index");
        }
        return "/site/operate-result";
    }

    @RequestMapping(path = "/kaptcha", method = RequestMethod.GET)
    public void getKaptcha(HttpServletResponse response, HttpSession session){
        // 生成验证码
        String text = kaptchaProducer.createText();
        BufferedImage image = kaptchaProducer.createImage(text);

        // 将验证码存入session
        session.setAttribute("kaptcha", text);

        // 将图片输出给浏览器
        response.setContentType("image/png");
        try {
            OutputStream os = response.getOutputStream();
            ImageIO.write(image, "png", os);
        } catch (IOException e) {
            logger.error("响应验证码失败", e);
        }
    }

    @RequestMapping(path = "/code", method = RequestMethod.GET)
    public ResponseEntity<String> getCode(HttpSession session, @RequestParam String email, Model model){
        // 验证邮箱及用户
        if(StringUtils.isBlank(email) || userService.findUserByEmail(email)==null){
            model.addAttribute("userMsg", "请检查邮箱输入值！");
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        // 生成六位验证码
        String code = CommunityUtil.generateUUID()
                .replaceAll("-","").substring(0,6);
        // 存储到session中
        session.setAttribute("code", code);
        // 设置五分钟过时时间
        session.setAttribute("codeExpiredAt", new Date(System.currentTimeMillis()+5*60*1000).toString());
        // 发送邮件
        userService.sendCode(email, code);
        // 返回200 OK状态和成功消息
        return ResponseEntity.ok("验证码发送成功！");
    }

    @RequestMapping(path="/login", method = RequestMethod.POST)
    public String login(String username, String password, String code, boolean remember,
                        Model model, HttpSession session, HttpServletResponse response){
        // 检查验证码
        String kaptcha = (String) session.getAttribute("kaptcha");
        if(StringUtils.isBlank(kaptcha) || StringUtils.isBlank(code) || !kaptcha.equalsIgnoreCase(code)){
            model.addAttribute("codeMsg", "验证码不正确！");
            return "/site/login";
        }

        // 检查账号密码
        int expiredSeconds = remember? REMEMBER_EXPIRED_SECONDS : DEFAULT_EXPIRED_SECONDS;
        Map<String, Object> map = userService.login(username, password, expiredSeconds);

        if(map.containsKey("ticket")){ // 登陆成功
            // 发送ticket到客户端
            Cookie cookie = new Cookie("ticket", map.get("ticket").toString());
            cookie.setPath(contextPath); // cookie有效的路径：整个项目
            cookie.setMaxAge(expiredSeconds);
            response.addCookie(cookie);
            return "redirect:/index";
        }else{
            model.addAttribute("usernameMsg", map.get("usernameMsg"));
            model.addAttribute("passwordMsg", map.get("passwordMsg"));
            return "/site/login";
        }
    }

    @RequestMapping(path="/logout", method=RequestMethod.GET)
    public String logout(@CookieValue("ticket") String ticket){
        userService.logout(ticket);
        return "redirect:/login"; // 默认重定向到login GET
    }

    @RequestMapping(path="/forget", method=RequestMethod.POST)
    public String forget(String email, String code, String password,
                         HttpSession session, Model model){

        // 验证验证码
        String verifyCode = session.getAttribute("code").toString();
        String codeExpiredAt = session.getAttribute("codeExpiredAt").toString();
        SimpleDateFormat format = new SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy");
        try {
            Date expiredTime = format.parse(codeExpiredAt);
            if(expiredTime.before(new Date())){
                model.addAttribute("codeMsg", "验证码已过时！");
                return "/site/forget";
            }
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
        if(!code.equalsIgnoreCase(verifyCode)){
            model.addAttribute("codeMsg", "验证码不正确！");
            return "/site/forget";
        }

        // 验证用户数据
        Map<String, Object> map = userService.forgetPassword(email, password);
        if(map.containsKey("userMsg")){
            model.addAttribute("userMsg", map.get("userMsg"));
            return "/site/forget";
        }
        if(map.containsKey("passwordMsg")){
            model.addAttribute("passwordMsg", map.get("passwordMsg"));
            return "/site/forget";
        }

        // 回到登录界面
        return "redirect:/login";
    }
}
