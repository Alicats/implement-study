package cn.xej.api.controller;

import cn.xej.api.request.CreateUserRequest;
import cn.xej.api.response.CreateUserResponse;
import cn.xej.api.service.UserService;
import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@Slf4j
@RestController
@RequestMapping("/api/users")
public class UserController {
    
    @Autowired
    private UserService userService;
    
//    /**
//     * 获取所有用户
//     * @return 用户列表
//     */
//    @GetMapping
//    public List<User> getAllUsers() {
//        return userService.getAllUsers();
//    }
//
//    /**
//     * 根据ID获取用户
//     * @param id 用户ID
//     * @return 用户信息
//     */
//    @GetMapping("/{id}")
//    public ResponseEntity<User> getUserById(@PathVariable Long id) {
//        Optional<User> user = userService.getUserById(id);
//        if (user.isPresent()) {
//            return ResponseEntity.ok(user.get());
//        } else {
//            return ResponseEntity.notFound().build();
//        }
//    }
    

    @PostMapping("CreateUser")
    public CreateUserResponse createUser(@Validated @RequestBody CreateUserRequest request) {
        log.info("api create user request:{}", JSON.toJSONString(request));
        return new CreateUserResponse("alicat-123");
    }
    
//    /**
//     * 更新用户信息
//     * @param id 用户ID
//     * @param user 更新的用户信息
//     * @return 更新后的用户信息
//     */
//    @PutMapping("/{id}")
//    public ResponseEntity<User> updateUser(@PathVariable Long id, @RequestBody User user) {
//        Optional<User> updatedUser = userService.updateUser(id, user);
//        if (updatedUser.isPresent()) {
//            return ResponseEntity.ok(updatedUser.get());
//        } else {
//            return ResponseEntity.notFound().build();
//        }
//    }
//
//    /**
//     * 删除用户
//     * @param id 用户ID
//     * @return 是否删除成功
//     */
//    @DeleteMapping("/{id}")
//    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
//        boolean deleted = userService.deleteUser(id);
//        if (deleted) {
//            return ResponseEntity.noContent().build();
//        } else {
//            return ResponseEntity.notFound().build();
//        }
//    }
}
