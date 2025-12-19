package cn.xej.api.service;

import cn.xej.api.model.User;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class UserService {
    
    private final Map<Long, User> userMap = new ConcurrentHashMap<>();
    private final AtomicLong counter = new AtomicLong();
    
    public UserService() {
        // 初始化一些用户数据
        userMap.put(counter.incrementAndGet(), new User(counter.get(), "张三", "zhangsan@example.com"));
        userMap.put(counter.incrementAndGet(), new User(counter.get(), "李四", "lisi@example.com"));
        userMap.put(counter.incrementAndGet(), new User(counter.get(), "王五", "wangwu@example.com"));
    }
    
    public List<User> getAllUsers() {
        return new ArrayList<>(userMap.values());
    }
    
    public Optional<User> getUserById(Long id) {
        return Optional.ofNullable(userMap.get(id));
    }
    
    public User createUser(User user) {
        Long id = counter.incrementAndGet();
        user.setId(id);
        userMap.put(id, user);
        return user;
    }
    
    public Optional<User> updateUser(Long id, User user) {
        if (userMap.containsKey(id)) {
            user.setId(id);
            userMap.put(id, user);
            return Optional.of(user);
        } else {
            return Optional.empty();
        }
    }
    
    public boolean deleteUser(Long id) {
        return userMap.remove(id) != null;
    }
}
