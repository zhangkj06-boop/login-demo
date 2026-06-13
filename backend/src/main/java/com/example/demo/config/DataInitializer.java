package com.example.demo.config;

import com.example.demo.entity.Game;
import com.example.demo.entity.RolePermission;
import com.example.demo.entity.User;
import com.example.demo.repository.GameRepository;
import com.example.demo.repository.RolePermissionRepository;
import com.example.demo.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * 数据初始化类。
 * <p>
 * 实现 {@link CommandLineRunner} 接口，Spring Boot 在应用启动完成、上下文就绪后，
 * 会自动调用 {@link #run(String...)} 方法执行一次性初始化逻辑。
 * </p>
 * <p>
 * 当前职责：当用户表为空时，自动创建 3 个默认账号（admin/user/test），
 * 避免首次部署后出现无账号可登录的情况。
 * </p>
 * <p>
 * 使用 {@link Component} 标记，由 Spring 自动扫描并实例化。
 * </p>
 */
@Component
public class DataInitializer implements CommandLineRunner {

    /** 日志记录器 */
    private static final Logger logger = LoggerFactory.getLogger(DataInitializer.class);

    /**
     * 用户数据访问接口。
     * 通过构造器注入，用于检查用户表状态及创建初始数据。
     */
    private final UserRepository userRepository;

    /**
     * 游戏数据访问接口。
     * 通过构造器注入，用于检查游戏表状态及创建初始游戏数据。
     */
    private final GameRepository gameRepository;

    /**
     * 角色权限数据访问接口。
     * 通过构造器注入，用于检查权限表状态及创建初始权限数据。
     */
    private final RolePermissionRepository rolePermissionRepository;

    /**
     * 密码加密器。
     * 通过构造器注入，用于对初始密码进行 BCrypt 哈希后再存储。
     */
    private final BCryptPasswordEncoder passwordEncoder;

    public DataInitializer(UserRepository userRepository,
                           GameRepository gameRepository,
                           RolePermissionRepository rolePermissionRepository,
                           BCryptPasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.gameRepository = gameRepository;
        this.rolePermissionRepository = rolePermissionRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * 应用启动后执行的数据初始化逻辑。
     * <p>
     * 执行流程：
     * <ol>
     *   <li>检查 userRepository.count() 是否为 0（表为空）</li>
     *   <li>若为空，依次创建 admin、user、test 三个默认用户</li>
     *   <li>密码经 passwordEncoder.encode() 加密后存入数据库</li>
     *   <li>若已有数据，则跳过，避免重复插入或覆盖用户修改</li>
     * </ol>
     * </p>
     *
     * @param args 启动参数（未使用）
     */
    @Override
    public void run(String... args) {
        logger.info("开始执行数据初始化...");
        if (userRepository.count() == 0) {
            logger.info("用户表为空，开始创建默认用户数据");
            userRepository.save(new User("admin", passwordEncoder.encode("123456")));
            logger.debug("创建默认管理员用户: admin");
            userRepository.save(new User("user", passwordEncoder.encode("password")));
            logger.debug("创建普通用户: user");
            userRepository.save(new User("test", passwordEncoder.encode("test123")));
            logger.debug("创建测试用户: test");
            logger.info("默认用户数据创建完成，共创建3个用户");
        } else {
            logger.info("用户表中已有数据，跳过默认用户创建");
        }

        // 初始化默认游戏数据
        if (gameRepository.count() == 0) {
            logger.info("游戏表为空，开始创建默认游戏数据");
            gameRepository.save(new Game("坦克大战", "tank-battle", "经典街机坦克对战游戏，操控坦克击败敌方部队。", null, 1));
            logger.debug("创建默认游戏: 坦克大战");
            gameRepository.save(new Game("弹珠游戏", "pinball", "经典弹珠台游戏，控制挡板让弹珠持续弹跳得分。", null, 2));
            logger.debug("创建默认游戏: 弹珠游戏");
            logger.info("默认游戏数据创建完成，共创建2个游戏");
        } else {
            logger.info("游戏表中已有数据，跳过默认游戏创建");
        }

        // 初始化默认角色权限数据
        if (rolePermissionRepository.count() == 0) {
            logger.info("权限表为空，开始创建默认角色权限数据");

            // 管理员权限
            rolePermissionRepository.save(new RolePermission("ADMIN", "users", "用户管理", true, true, "&#128100;", 1));
            rolePermissionRepository.save(new RolePermission("ADMIN", "students", "学生管理", true, true, "&#127891;", 2));
            rolePermissionRepository.save(new RolePermission("ADMIN", "teachers", "教师管理", true, true, "&#128221;", 3));
            rolePermissionRepository.save(new RolePermission("ADMIN", "inventory", "仓库管理", true, true, "&#128230;", 4));
            rolePermissionRepository.save(new RolePermission("ADMIN", "complaints", "投诉反馈", false, false, "&#128172;", 5));
            rolePermissionRepository.save(new RolePermission("ADMIN", "complaintManage", "投诉管理", true, true, "&#128203;", 6));
            rolePermissionRepository.save(new RolePermission("ADMIN", "roles", "角色权限", true, false, "&#128275;", 7));
            rolePermissionRepository.save(new RolePermission("ADMIN", "games", "游戏中心", true, false, "&#127918;", 8));

            // 教师权限
            rolePermissionRepository.save(new RolePermission("TEACHER", "users", "用户管理", false, false, "&#128100;", 1));
            rolePermissionRepository.save(new RolePermission("TEACHER", "students", "学生管理", true, true, "&#127891;", 2));
            rolePermissionRepository.save(new RolePermission("TEACHER", "teachers", "教师管理", true, true, "&#128221;", 3));
            rolePermissionRepository.save(new RolePermission("TEACHER", "inventory", "仓库管理", true, true, "&#128230;", 4));
            rolePermissionRepository.save(new RolePermission("TEACHER", "complaints", "投诉反馈", false, false, "&#128172;", 5));
            rolePermissionRepository.save(new RolePermission("TEACHER", "complaintManage", "投诉管理", false, false, "&#128203;", 6));
            rolePermissionRepository.save(new RolePermission("TEACHER", "roles", "角色权限", true, false, "&#128275;", 7));
            rolePermissionRepository.save(new RolePermission("TEACHER", "games", "游戏中心", true, false, "&#127918;", 8));

            // 学生权限
            rolePermissionRepository.save(new RolePermission("STUDENT", "users", "用户管理", false, false, "&#128100;", 1));
            rolePermissionRepository.save(new RolePermission("STUDENT", "students", "学生管理", false, false, "&#127891;", 2));
            rolePermissionRepository.save(new RolePermission("STUDENT", "teachers", "教师管理", false, false, "&#128221;", 3));
            rolePermissionRepository.save(new RolePermission("STUDENT", "inventory", "仓库管理", false, false, "&#128230;", 4));
            rolePermissionRepository.save(new RolePermission("STUDENT", "complaints", "投诉反馈", true, true, "&#128172;", 5));
            rolePermissionRepository.save(new RolePermission("STUDENT", "complaintManage", "投诉管理", false, false, "&#128203;", 6));
            rolePermissionRepository.save(new RolePermission("STUDENT", "roles", "角色权限", true, false, "&#128275;", 7));
            rolePermissionRepository.save(new RolePermission("STUDENT", "games", "游戏中心", true, false, "&#127918;", 8));

            logger.info("默认角色权限数据创建完成，共创建27条权限记录");
        } else {
            logger.info("权限表中已有数据，跳过默认权限创建");
        }

        logger.info("数据初始化执行完毕");
    }
}
