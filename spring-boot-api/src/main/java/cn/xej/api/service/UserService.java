package cn.xej.api.service;

import cn.xej.api.model.User;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class UserService {
    
    private List<User> users = new ArrayList<>();
    
    public UserService() {
        // 初始化一些用户数据
        users.add(new User(1L, "张三", "zhangsan@example.com"));
        users.add(new User(2L, "李四", "lisi@example.com"));
        users.add(new User(3L, "王五", "wangwu@example.com"));
    }
    
    public List<User> getAllUsers() {
        return users;
    }
    
    public Optional<User> getUserById(Long id) {
        return users.stream().filter(user -> user.getId().equals(id)).findFirst();
    }
    
    public User createUser(User user) {
        // 设置ID（简单起见，实际项目中应该使用更复杂的ID生成策略）
        user.setId((long) (users.size() + 1));
        users.add(user);
        return user;
    }
    
    public Optional<User> updateUser(Long id, User updatedUser) {
        Optional<User> existingUser = getUserById(id);
        if (existingUser.isPresent()) {
            User user = existingUser.get();
            user.setName(updatedUser.getName());
            user.setEmail(updatedUser.getEmail());
            return Optional.of(user);
        }
        return Optional.empty();
    }
    
    public boolean deleteUser(Long id) {
        return users.removeIf(user -> user.getId().equals(id));
    }
}
