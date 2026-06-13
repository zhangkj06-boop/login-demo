package com.example.demo.controller;

import com.example.demo.entity.Student;
import com.example.demo.entity.Teacher;
import com.example.demo.entity.User;
import com.example.demo.model.LoginRequest;
import com.example.demo.model.LoginResponse;
import com.example.demo.repository.StudentRepository;
import com.example.demo.repository.TeacherRepository;
import com.example.demo.repository.UserRepository;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 认证控制器。
 * <p>
 * 提供登录、注册、密码修改及获取当前用户信息等认证相关的 REST API。
 * </p>
 * <p>
 * 多角色登录设计：
 * <ul>
 *   <li>管理员（user）→ 查询 users 表，通过 username 匹配</li>
 *   <li>学生（student）→ 查询 students 表，通过 studentId 或 name 匹配</li>
 *   <li>教师（teacher）→ 查询 teachers 表，通过 teacherId 或 name 匹配</li>
 * </ul>
 * 登录成功后返回 UUID 作为 Token（当前为无状态设计，未使用 JWT）。
 * </p>
 */
@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class AuthController {

    /** 日志记录器 */
    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    /** 用户数据访问接口 */
    private final UserRepository userRepository;
    /** 学生数据访问接口 */
    private final StudentRepository studentRepository;
    /** 教师数据访问接口 */
    private final TeacherRepository teacherRepository;
    /** 密码加密器 */
    private final BCryptPasswordEncoder passwordEncoder;

    public AuthController(UserRepository userRepository,
                          StudentRepository studentRepository,
                          TeacherRepository teacherRepository,
                          BCryptPasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.studentRepository = studentRepository;
        this.teacherRepository = teacherRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * 用户登录（支持多角色）。
     * <p>
     * 根据 loginType 路由到不同的登录逻辑：
     * <ul>
     *   <li>student → {@link #loginStudent(String, String)}</li>
     *   <li>teacher → {@link #loginTeacher(String, String)}</li>
     *   <li>其他（默认 user）→ {@link #loginUser(String, String)}</li>
     * </ul>
     * </p>
     *
     * @param request 登录请求（含 username、password、loginType）
     * @return 登录响应（含 Token、角色等）
     */
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        logger.info("用户登录，用户名: {}, 登录类型: {}", request.getUsername(), request.getLoginType());
        String username = request.getUsername();
        String password = request.getPassword();
        String loginType = request.getLoginType();

        if (loginType == null || loginType.isBlank()) {
            loginType = "user";
        }

        switch (loginType) {
            case "student":
                return loginStudent(username, password);
            case "teacher":
                return loginTeacher(username, password);
            default:
                return loginUser(username, password);
        }
    }

    /**
     * 管理员登录。
     * <p>
     * 通过 username 查询 users 表，使用 BCrypt 校验密码。
     * </p>
     *
     * @param username 用户名
     * @param password 密码（明文）
     * @return 登录成功返回 Token 和角色信息；失败返回 401
     */
    private ResponseEntity<LoginResponse> loginUser(String username, String password) {
        logger.info("管理员登录，用户名: {}", username);
        User user = userRepository.findByUsername(username).orElse(null);
        if (user == null) {
            logger.warn("管理员登录失败，用户不存在: {}", username);
            return ResponseEntity.status(401).body(LoginResponse.error("用户不存在"));
        }
        if (!passwordEncoder.matches(password, user.getPassword())) {
            logger.warn("管理员登录失败，密码错误: {}", username);
            return ResponseEntity.status(401).body(LoginResponse.error("密码错误"));
        }
        String token = UUID.randomUUID().toString().replace("-", "");
        String role = user.getRole();
        if (role == null || role.isBlank()) {
            role = "ADMIN";
        }
        logger.info("管理员登录成功: {}", username);
        return ResponseEntity.ok(LoginResponse.success("登录成功", token, username, user.getAvatar(), role));
    }

    /**
     * 学生登录。
     * <p>
     * 优先通过 studentId 匹配，若未找到则尝试通过 name 匹配（取第一条）。
     * </p>
     *
     * @param account  学号或姓名
     * @param password 密码（明文）
     * @return 登录成功返回 Token 和 STUDENT 角色；失败返回 401
     */
    private ResponseEntity<LoginResponse> loginStudent(String account, String password) {
        logger.info("学生登录，账号: {}", account);
        Student student = studentRepository.findByStudentId(account).orElse(null);
        if (student == null) {
            student = studentRepository.findByName(account).stream().findFirst().orElse(null);
        }
        if (student == null) {
            logger.warn("学生登录失败，学生不存在: {}", account);
            return ResponseEntity.status(401).body(LoginResponse.error("学生不存在"));
        }
        if (!passwordEncoder.matches(password, student.getPassword())) {
            logger.warn("学生登录失败，密码错误: {}", account);
            return ResponseEntity.status(401).body(LoginResponse.error("密码错误"));
        }
        String token = UUID.randomUUID().toString().replace("-", "");
        logger.info("学生登录成功: {}", account);
        return ResponseEntity.ok(LoginResponse.success("登录成功", token, student.getName(), null, "STUDENT"));
    }

    /**
     * 教师登录。
     * <p>
     * 优先通过 teacherId 匹配，若未找到则尝试通过 name 匹配（取第一条）。
     * </p>
     *
     * @param account  教师ID或姓名
     * @param password 密码（明文）
     * @return 登录成功返回 Token 和 TEACHER 角色；失败返回 401
     */
    private ResponseEntity<LoginResponse> loginTeacher(String account, String password) {
        logger.info("教师登录，账号: {}", account);
        Teacher teacher = teacherRepository.findByTeacherId(account).orElse(null);
        if (teacher == null) {
            teacher = teacherRepository.findByName(account).stream().findFirst().orElse(null);
        }
        if (teacher == null) {
            logger.warn("教师登录失败，教师不存在: {}", account);
            return ResponseEntity.status(401).body(LoginResponse.error("教师不存在"));
        }
        if (!passwordEncoder.matches(password, teacher.getPassword())) {
            logger.warn("教师登录失败，密码错误: {}", account);
            return ResponseEntity.status(401).body(LoginResponse.error("密码错误"));
        }
        String token = UUID.randomUUID().toString().replace("-", "");
        logger.info("教师登录成功: {}", account);
        return ResponseEntity.ok(LoginResponse.success("登录成功", token, teacher.getName(), null, "TEACHER"));
    }

    /**
     * 用户注册（创建新的管理员账号）。
     *
     * @param request 注册请求（含 username、password）
     * @return 注册结果
     */
    @PostMapping("/register")
    public ResponseEntity<LoginResponse> register(@Valid @RequestBody LoginRequest request) {
        logger.info("用户注册，用户名: {}", request.getUsername());
        String username = request.getUsername();
        if (userRepository.existsByUsername(username)) {
            logger.warn("用户注册失败，用户名已存在: {}", username);
            return ResponseEntity.badRequest().body(LoginResponse.error("用户名已存在"));
        }
        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole("ADMIN");
        userRepository.save(user);
        logger.info("用户注册成功: {}", username);
        return ResponseEntity.ok(LoginResponse.success("注册成功", null, username));
    }

    /**
     * 获取当前登录用户信息。
     * <p>
     * 当前仅做 Token 存在性校验，返回 authenticated=true。
     * 实际项目中可扩展为解析 Token 并返回用户详细信息。
     * </p>
     *
     * @param token 认证 Token（请求头 Authorization）
     * @return 用户信息
     */
    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(@RequestHeader(value = "Authorization", required = false) String token) {
        logger.info("获取当前用户信息，token: {}", token);
        if (token == null || token.isEmpty()) {
            logger.warn("获取当前用户信息失败，未登录");
            return ResponseEntity.status(401).body(LoginResponse.error("未登录"));
        }
        Map<String, Object> result = new HashMap<>();
        result.put("authenticated", true);
        logger.info("获取当前用户信息成功");
        return ResponseEntity.ok(result);
    }

    /**
     * 修改密码。
     * <p>
     * 支持修改用户（user）、学生（student）、教师（teacher）三种类型的密码。
     * 需验证原密码正确性，新密码长度不少于 6 位。
     * </p>
     *
     * @param body 请求体（含 type、id、oldPassword、newPassword）
     * @return 修改结果
     */
    @PostMapping("/change-password")
    public ResponseEntity<Map<String, Object>> changePassword(@RequestBody Map<String, String> body) {
        logger.info("修改密码，类型: {}, ID: {}", body.get("type"), body.get("id"));
        Map<String, Object> result = new HashMap<>();
        String type = body.get("type");
        String id = body.get("id");
        String oldPassword = body.get("oldPassword");
        String newPassword = body.get("newPassword");

        if (type == null || id == null || oldPassword == null || newPassword == null) {
            logger.warn("修改密码失败，参数不完整");
            result.put("success", false);
            result.put("message", "参数不完整");
            return ResponseEntity.badRequest().body(result);
        }
        if (newPassword.length() < 6) {
            logger.warn("修改密码失败，新密码长度不能少于6位");
            result.put("success", false);
            result.put("message", "新密码长度不能少于6位");
            return ResponseEntity.badRequest().body(result);
        }

        switch (type) {
            case "user":
                User user = userRepository.findByUsername(id).orElse(null);
                if (user == null) {
                    logger.warn("修改密码失败，用户不存在: {}", id);
                    result.put("success", false);
                    result.put("message", "用户不存在");
                    return ResponseEntity.status(404).body(result);
                }
                if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
                    logger.warn("修改密码失败，原密码错误: {}", id);
                    result.put("success", false);
                    result.put("message", "原密码错误");
                    return ResponseEntity.badRequest().body(result);
                }
                user.setPassword(passwordEncoder.encode(newPassword));
                userRepository.save(user);
                break;
            case "student":
                Student student = studentRepository.findByStudentId(id).orElse(null);
                if (student == null) {
                    logger.warn("修改密码失败，学生不存在: {}", id);
                    result.put("success", false);
                    result.put("message", "学生不存在");
                    return ResponseEntity.status(404).body(result);
                }
                if (!passwordEncoder.matches(oldPassword, student.getPassword())) {
                    logger.warn("修改密码失败，原密码错误: {}", id);
                    result.put("success", false);
                    result.put("message", "原密码错误");
                    return ResponseEntity.badRequest().body(result);
                }
                student.setPassword(passwordEncoder.encode(newPassword));
                studentRepository.save(student);
                break;
            case "teacher":
                Teacher teacher = teacherRepository.findByTeacherId(id).orElse(null);
                if (teacher == null) {
                    logger.warn("修改密码失败，教师不存在: {}", id);
                    result.put("success", false);
                    result.put("message", "教师不存在");
                    return ResponseEntity.status(404).body(result);
                }
                if (!passwordEncoder.matches(oldPassword, teacher.getPassword())) {
                    logger.warn("修改密码失败，原密码错误: {}", id);
                    result.put("success", false);
                    result.put("message", "原密码错误");
                    return ResponseEntity.badRequest().body(result);
                }
                teacher.setPassword(passwordEncoder.encode(newPassword));
                teacherRepository.save(teacher);
                break;
            default:
                logger.warn("修改密码失败，无效的类型: {}", type);
                result.put("success", false);
                result.put("message", "无效的类型");
                return ResponseEntity.badRequest().body(result);
        }

        result.put("success", true);
        result.put("message", "密码修改成功");
        logger.info("密码修改成功，类型: {}, ID: {}", type, id);
        return ResponseEntity.ok(result);
    }
}
