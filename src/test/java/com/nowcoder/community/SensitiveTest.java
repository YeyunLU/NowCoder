package com.nowcoder.community;

import com.nowcoder.community.util.SensitiveFilter;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
@ContextConfiguration(classes = CommunityApplication.class)
public class SensitiveTest {

    @Autowired
    SensitiveFilter sensitiveFilter;

    @Test
    public void testSensitiveFilter(){
        String text = "这里可以赌博，哈哈哈！";
        System.out.println(sensitiveFilter.filter(text));
    }

    @Test
    public void testSensitiveFilterWithSymbol(){
        String text = "这里可以☆赌☆博☆，哈哈哈！";
        System.out.println(sensitiveFilter.filter(text));
    }
}
