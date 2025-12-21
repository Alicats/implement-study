package cn.xej.api.service;

import cn.xej.api.model.User;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class UserService {
    
    private final AtomicLong counter = new AtomicLong();

    
    public List<User> getAllUsers() {
        List<User> users = new ArrayList<>();
        User user1 = new User();
        user1.setUuid(UUID.randomUUID().toString());
        user1.setName("张三");
        user1.setEmail("20320@qq.com");

        User user2 = new User();
        user2.setUuid(UUID.randomUUID().toString());
        user2.setName("李四");
        user2.setEmail("10320@qq.com");

        users.add(user1);
        users.add(user2);
        return users;
    }
}
