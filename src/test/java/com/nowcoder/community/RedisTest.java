package com.nowcoder.community;

import com.mysql.cj.util.TimeUtil;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.BoundValueOperations;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.concurrent.TimeUnit;

@RunWith(SpringRunner.class)
@SpringBootTest
@ContextConfiguration(classes = CommunityApplication.class)
public class RedisTest {

    @Autowired
    private RedisTemplate redisTemplate;

    @Test
    public void testString() {
        String redisKey = "count";
        redisTemplate.opsForValue().set(redisKey, 1);
        System.out.println(redisTemplate.opsForValue().get(redisKey));
        System.out.println(redisTemplate.opsForValue().increment(redisKey,1));
        System.out.println(redisTemplate.opsForValue().decrement(redisKey));
    }

    @Test
    public void testHash() {
        String redisKey = "test:user";
        redisTemplate.opsForHash().put(redisKey, "id", 1);
        redisTemplate.opsForHash().put(redisKey, "name", "Lucy");
        System.out.println(redisTemplate.opsForHash().get(redisKey, "id"));
        System.out.println(redisTemplate.opsForHash().get(redisKey, "name"));
    }

    @Test
    public void testList() {
        String key = "test:id";
        redisTemplate.opsForList().leftPush(key, 101);
        redisTemplate.opsForList().leftPush(key, 102);
        redisTemplate.opsForList().leftPush(key, 103);
        System.out.println(redisTemplate.opsForList().size(key));
        System.out.println(redisTemplate.opsForList().index(key, 0));
        System.out.println(redisTemplate.opsForList().range(key, 0 ,2));
        System.out.println(redisTemplate.opsForList().leftPop(key));
        System.out.println(redisTemplate.opsForList().leftPop(key));
        System.out.println(redisTemplate.opsForList().leftPop(key));
    }

    @Test
    public void testSet(){
        String key = "test:teachers";
        redisTemplate.opsForSet().add(key, "Lucy","Andrew","Cindy","Gary");
        System.out.println(redisTemplate.opsForSet().size(key));
        System.out.println(redisTemplate.opsForSet().pop(key));
        System.out.println(redisTemplate.opsForSet().members(key));
    }

    @Test
    public void testSortedSet() {
        String key = "test:students";
        redisTemplate.opsForZSet().add(key, "唐僧",100);
        redisTemplate.opsForZSet().add(key, "悟空",500);
        redisTemplate.opsForZSet().add(key, "沙僧",300);
        redisTemplate.opsForZSet().add(key, "八戒",200);
        System.out.println(redisTemplate.opsForZSet().zCard(key));
        System.out.println(redisTemplate.opsForZSet().score(key, "悟空"));
        System.out.println(redisTemplate.opsForZSet().reverseRank(key, "八戒"));
        System.out.println(redisTemplate.opsForZSet().range(key, 0 ,2));
        System.out.println(redisTemplate.opsForZSet().reverseRange(key, 0 ,2));
    }

    @Test
    public void testKey(){
        redisTemplate.delete("test:user");
        System.out.println(redisTemplate.hasKey("test:user"));
        redisTemplate.expire("test:students", 10, TimeUnit.SECONDS);
    }

    // 多次访问一个key
    @Test
    public void testBoundOperation() {
        String key = "test:count";
        BoundValueOperations valueOperations = redisTemplate.boundValueOps(key);
        // 不用再传入key
        System.out.println(valueOperations.get());
        valueOperations.increment();
        valueOperations.increment();
        valueOperations.increment();
        valueOperations.increment();
        valueOperations.increment();
        System.out.println(valueOperations.get());
    }

    // 编程式事务
    @Test
    public void testTransaction(){
        Object obj = redisTemplate.execute(new SessionCallback() {
            @Override
            public Object execute(RedisOperations operations) throws DataAccessException {
                String key = "test:tx";
                // 启用事务
                operations.multi();
                operations.opsForSet().add(key,"张三");
                operations.opsForSet().add(key,"李四");
                System.out.println(operations.opsForSet().members(key));
                // 提交事务
                return operations.exec();
            }
        });
        System.out.println(obj);
    }
}
