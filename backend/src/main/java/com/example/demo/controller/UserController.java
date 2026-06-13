package com.example.demo.controller;

import com.example.demo.entity.User;
import com.example.demo.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 用户（管理员）控制器。
 * <p>
 * 提供管理员用户的增删改查 REST API，包括头像更新。
 * </p>
 * <p>
 * 安全设计：密码字段通过 {@code @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)} 在响应中隐藏。
 * </p>
 */
@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "*")
public class UserController {

    /** 日志记录器 */
    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    /** 用户数据访问接口 */
    private final UserRepository userRepository;
    /** 密码加密器 */
    private final BCryptPasswordEncoder passwordEncoder;

    public UserController(UserRepository userRepository, BCryptPasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * 查询所有用户列表。
     *
     * @return 用户列表（密码字段已隐藏）
     */
    @GetMapping
    public ResponseEntity<List<User>> listUsers() {
        logger.info("查询所有用户列表");
        List<User> users = userRepository.findAll();
        logger.info("查询用户列表成功，共 {} 条记录", users.size());
        return ResponseEntity.ok(users);
    }

    /**
     * 根据ID查询用户详情。
     *
     * @param id 用户主键
     * @return 用户详情；不存在返回 404
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getUser(@PathVariable Long id) {
        logger.info("查询用户详情，ID: {}", id);
        return userRepository.findById(id)
                .map(user -> {
                    logger.info("查询用户详情成功，ID: {}", id);
                    return ResponseEntity.ok(user);
                })
                .orElseGet(() -> {
                    logger.warn("查询用户详情失败，用户不存在，ID: {}", id);
                    return ResponseEntity.status(404).body((User) null);
                });
    }

    /**
     * 创建新用户（管理员账号）。
     *
     * @param user 用户实体（含 username、password）
     * @return 创建结果
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> createUser(@RequestBody User user) {
        logger.info("创建用户，用户名: {}", user.getUsername());
        Map<String, Object> result = new HashMap<>();
        if (user.getUsername() == null || user.getUsername().isBlank()) {
            logger.warn("创建用户失败，用户名不能为空");
            result.put("success", false);
            result.put("message", "用户名不能为空");
            return ResponseEntity.badRequest().body(result);
        }
        if (user.getPassword() == null || user.getPassword().length() < 6) {
            logger.warn("创建用户失败，密码长度不能少于6位");
            result.put("success", false);
            result.put("message", "密码长度不能少于6位");
            return ResponseEntity.badRequest().body(result);
        }
        if (userRepository.existsByUsername(user.getUsername())) {
            logger.warn("创建用户失败，用户名已存在: {}", user.getUsername());
            result.put("success", false);
            result.put("message", "用户名已存在");
            return ResponseEntity.badRequest().body(result);
        }
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        User saved = userRepository.save(user);
        logger.info("创建用户成功，ID: {}", saved.getId());
        result.put("success", true);
        result.put("message", "创建成功");
        result.put("id", saved.getId());
        return ResponseEntity.ok(result);
    }

    /**
     * 更新用户信息（部分更新）。
     * <p>
     * 支持修改用户名和密码。若修改用户名，需再次校验唯一性。
     * </p>
     *
     * @param id   用户主键
     * @param user 更新的内容
     * @return 更新结果
     */
    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateUser(@PathVariable Long id, @RequestBody User user) {
        logger.info("更新用户，ID: {}", id);
        Map<String, Object> result = new HashMap<>();
        User existing = userRepository.findById(id).orElse(null);
        if (existing == null) {
            logger.warn("更新用户失败，用户不存在，ID: {}", id);
            result.put("success", false);
            result.put("message", "用户不存在");
            return ResponseEntity.status(404).body(result);
        }
        if (user.getUsername() != null && !user.getUsername().isBlank()
                && !user.getUsername().equals(existing.getUsername())) {
            if (userRepository.existsByUsername(user.getUsername())) {
                logger.warn("更新用户失败，用户名已存在: {}", user.getUsername());
                result.put("success", false);
                result.put("message", "用户名已存在");
                return ResponseEntity.badRequest().body(result);
            }
            existing.setUsername(user.getUsername());
        }
        if (user.getPassword() != null && !user.getPassword().isBlank()) {
            if (user.getPassword().length() < 6) {
                logger.warn("更新用户失败，密码长度不能少于6位");
                result.put("success", false);
                result.put("message", "密码长度不能少于6位");
                return ResponseEntity.badRequest().body(result);
            }
            existing.setPassword(passwordEncoder.encode(user.getPassword()));
        }
        userRepository.save(existing);
        logger.info("更新用户成功，ID: {}", id);
        result.put("success", true);
        result.put("message", "更新成功");
        return ResponseEntity.ok(result);
    }

    /**
     * 删除用户。
     *
     * @param id 用户主键
     * @return 删除结果
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteUser(@PathVariable Long id) {
        logger.info("删除用户，ID: {}", id);
        Map<String, Object> result = new HashMap<>();
        if (!userRepository.existsById(id)) {
            logger.warn("删除用户失败，用户不存在，ID: {}", id);
            result.put("success", false);
            result.put("message", "用户不存在");
            return ResponseEntity.status(404).body(result);
        }
        userRepository.deleteById(id);
        logger.info("删除用户成功，ID: {}", id);
        result.put("success", true);
        result.put("message", "删除成功");
        return ResponseEntity.ok(result);
    }

    /**
     * 更新用户头像。
     *
     * @param id  用户主键
     * @param body 请求体（含 avatar URL）
     * @return 更新结果
     */
    @PutMapping("/{id}/avatar")
    public ResponseEntity<Map<String, Object>> updateAvatar(@PathVariable Long id, @RequestBody Map<String, String> body) {
        logger.info("更新用户头像，ID: {}", id);
        Map<String, Object> result = new HashMap<>();
        User existing = userRepository.findById(id).orElse(null);
        if (existing == null) {
            logger.warn("更新用户头像失败，用户不存在，ID: {}", id);
            result.put("success", false);
            result.put("message", "用户不存在");
            return ResponseEntity.status(404).body(result);
        }
        existing.setAvatar(body.get("avatar"));
        userRepository.save(existing);
        logger.info("更新用户头像成功，ID: {}", id);
        result.put("success", true);
        result.put("message", "头像更新成功");
        result.put("avatar", existing.getAvatar());
        return ResponseEntity.ok(result);
    }
}
