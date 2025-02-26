package com.nowcoder.community.controller;

import com.nowcoder.community.annotation.LoginRequired;
import com.nowcoder.community.entity.User;
import com.nowcoder.community.service.UserService;
import com.nowcoder.community.util.CommunityUtil;
import com.nowcoder.community.util.HostHolder;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;

@Controller
@RequestMapping("/user")
public class UserController {

    private static Logger logger = LoggerFactory.getLogger(UserController.class);

    @Value("${community.path.upload}")
    private String uploadPath;

    @Value("${community.path.domain}")
    private String domain;

    @Value("${server.servlet.context-path}")
    private String contextPath;

    @Autowired
    private UserService userService;

    @Autowired
    private HostHolder hostHolder;

    @LoginRequired
    @RequestMapping(value = "/setting", method = RequestMethod.GET)
    public String getSettingPage(){
        return "/site/setting";
    }

    @LoginRequired
    @RequestMapping(path="/upload", method = RequestMethod.POST)
    public String uploadHeader(MultipartFile headerImage, Model model){
        if(headerImage.isEmpty()){
            model.addAttribute("error", "您还没有选择图片！");
            return "/site/setting";
        }
        String fileName = headerImage.getOriginalFilename();
        String suffix = fileName.substring(fileName.lastIndexOf('.'));
        if(StringUtils.isBlank(suffix)){
            model.addAttribute("error","文件的格式不正确！");
            return "/site/setting";
        }
        // 生成随机文件名
        fileName = CommunityUtil.generateUUID() + suffix;
        // 确定文件存放的路径
        File file = new File(uploadPath+"/"+fileName);
        try {
            headerImage.transferTo(file);
        } catch (IOException e) {
            logger.error("上传文件失败：" + e.getMessage());
            throw new RuntimeException("上传文件失败：", e);
        }
        // 更新当前用户图像的路径（web访问路径）
        // http://localhost:8080/community/user/header/xxx.png
        User user = hostHolder.getUser();
        String headerUrl = domain + contextPath + "/user/header/" + fileName;
        userService.updateHeader(user.getId(), headerUrl);
        return "redirect:/index";
    }

    @RequestMapping(path="/header/{fileName}", method = RequestMethod.GET)
    public void getHeader(@PathVariable("fileName") String fileName, HttpServletResponse response){
        String filePath = uploadPath + "/" + fileName;
        String suffix = fileName.substring(fileName.lastIndexOf('.'));
        response.setContentType("image/"+suffix);
        try (
                FileInputStream fis = new FileInputStream(filePath);
                OutputStream os = response.getOutputStream();
                ){
            byte[] buffer = new byte[1024];
            int idx = 0;
            while((idx=fis.read(buffer))!=-1){
                os.write(buffer, 0, idx);
            }
        } catch (IOException e) {
            logger.error("读取头像失败" + e.getMessage());
        }
    }

    @LoginRequired
    @RequestMapping(path="/password", method = RequestMethod.POST)
    public String updatePassword(String oldPassword, String newPassword,
                                 String confirmPassword, Model model, @CookieValue("ticket") String ticket){
        // 验证输入合法性
        if(oldPassword.length()<8) {
            model.addAttribute("oldPwdMsg", "密码长度不能小于8位!");
            return "/site/setting";
        }
        if(newPassword.length()<8) {
            model.addAttribute("newPwdMsg", "密码长度不能小于8位!");
            return "/site/setting";
        }
        if(!confirmPassword.equals(newPassword)) {
            model.addAttribute("confirmPwdMsg", "两次输入的密码不一致!");
            return "/site/setting";
        }
        // 验证旧密码正确性
        User user = hostHolder.getUser();
        oldPassword = CommunityUtil.md5(oldPassword + user.getSalt());
        if(!user.getPassword().equals(oldPassword)){
            model.addAttribute("oldPwdMsg", "密码不正确！");
            return "/site/setting";
        }
        // 更新密码
        userService.updatePassword(user.getId(), newPassword);
        // 退出登录
        userService.logout(ticket);
        return "redirect:/login";
    }
}
