package com.example.demo.controller;

import com.example.demo.entity.RolePermission;
import com.example.demo.repository.RolePermissionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 角色权限控制器。
 * <p>
 * 提供角色权限管理的 REST API，包括查询所有权限、按角色查询、更新权限及初始化默认权限数据。
 * </p>
 * <p>
 * 本系统采用 RBAC（Role-Based Access Control）模型，
 * 每个角色（ADMIN / STUDENT / TEACHER）对应一组模块权限（canView / canEdit）。
 * 前端通过 /api/role-permissions/role/{roleName} 获取当前角色的权限列表，动态渲染导航菜单。
 * </p>
 */
@RestController
@RequestMapping("/api/role-permissions")
@CrossOrigin(origins = "*")
public class RolePermissionController {

    /** 日志记录器 */
    private static final Logger logger = LoggerFactory.getLogger(RolePermissionController.class);

    /** 角色权限数据访问接口 */
    private final RolePermissionRepository rolePermissionRepository;

    public RolePermissionController(RolePermissionRepository rolePermissionRepository) {
        this.rolePermissionRepository = rolePermissionRepository;
    }

    /**
     * 查询所有角色权限列表。
     *
     * @return 全部角色权限配置
     */
    @GetMapping
    public ResponseEntity<List<RolePermission>> listAll() {
        logger.info("查询所有角色权限列表");
        List<RolePermission> permissions = rolePermissionRepository.findAll();
        logger.info("查询角色权限列表成功，共 {} 条记录", permissions.size());
        return ResponseEntity.ok(permissions);
    }

    /**
     * 根据角色名称查询权限列表。
     * <p>
     * 前端登录成功后调用此接口，根据返回的权限列表决定显示哪些菜单项。
     * </p>
     *
     * @param roleName 角色名称（ADMIN / STUDENT / TEACHER）
     * @return 该角色的权限列表（按 sortOrder 升序）
     */
    @GetMapping("/role/{roleName}")
    public ResponseEntity<List<RolePermission>> listByRole(@PathVariable String roleName) {
        logger.info("根据角色查询权限，角色: {}", roleName);
        List<RolePermission> permissions = rolePermissionRepository.findByRoleNameOrderBySortOrderAsc(roleName);
        logger.info("查询角色权限成功，角色: {}, 共 {} 条记录", roleName, permissions.size());
        return ResponseEntity.ok(permissions);
    }

    /**
     * 更新角色权限。
     * <p>
     * 仅更新 canView 和 canEdit 字段，其他字段不可通过此接口修改。
     * </p>
     *
     * @param id         权限记录主键
     * @param permission 更新的权限值
     * @return 更新结果
     */
    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updatePermission(@PathVariable Long id, @RequestBody RolePermission permission) {
        logger.info("更新角色权限，ID: {}", id);
        Map<String, Object> result = new HashMap<>();
        RolePermission existing = rolePermissionRepository.findById(id).orElse(null);
        if (existing == null) {
            logger.warn("更新角色权限失败，权限记录不存在，ID: {}", id);
            result.put("success", false);
            result.put("message", "权限记录不存在");
            return ResponseEntity.status(404).body(result);
        }
        if (permission.getCanView() != null) {
            existing.setCanView(permission.getCanView());
        }
        if (permission.getCanEdit() != null) {
            existing.setCanEdit(permission.getCanEdit());
        }
        rolePermissionRepository.save(existing);
        logger.info("更新角色权限成功，ID: {}", id);
        result.put("success", true);
        result.put("message", "更新成功");
        return ResponseEntity.ok(result);
    }

    /**
     * 初始化默认角色权限数据。
     * <p>
     * 系统首次部署时调用，插入 ADMIN / STUDENT / TEACHER 三套默认权限配置。
     * 若权限数据已存在（count > 0），默认返回 400 及 "权限数据已存在" 提示，保证幂等性。
     * 可通过 {@code force=true} 强制清空并重新初始化。
     * </p>
     *
     * @param force 是否强制重新初始化（true 时先清空再插入）
     * @return 初始化结果
     */
    @PostMapping("/init")
    public ResponseEntity<Map<String, Object>> initDefaultPermissions(@RequestParam(defaultValue = "false") boolean force) {
        logger.info("初始化默认角色权限数据，force={}", force);
        Map<String, Object> result = new HashMap<>();
        if (rolePermissionRepository.count() > 0 && !force) {
            logger.warn("初始化角色权限失败，权限数据已存在");
            result.put("success", false);
            result.put("message", "权限数据已存在，如需覆盖请传 force=true");
            return ResponseEntity.badRequest().body(result);
        }
        if (force) {
            logger.info("强制重新初始化，先清空现有权限数据");
            rolePermissionRepository.deleteAll();
        }

        // 管理员权限：拥有所有模块的查看和编辑权限
        rolePermissionRepository.save(new RolePermission("ADMIN", "users", "用户管理", true, true, "&#128100;", 1));
        rolePermissionRepository.save(new RolePermission("ADMIN", "students", "学生管理", true, true, "&#127891;", 2));
        rolePermissionRepository.save(new RolePermission("ADMIN", "teachers", "教师管理", true, true, "&#128221;", 3));
        rolePermissionRepository.save(new RolePermission("ADMIN", "inventory", "仓库管理", true, true, "&#128230;", 4));
        rolePermissionRepository.save(new RolePermission("ADMIN", "complaints", "投诉反馈", false, false, "&#128172;", 5));
        rolePermissionRepository.save(new RolePermission("ADMIN", "complaintManage", "投诉管理", true, true, "&#128203;", 6));
        rolePermissionRepository.save(new RolePermission("ADMIN", "roles", "角色权限", true, false, "&#128275;", 7));
        rolePermissionRepository.save(new RolePermission("ADMIN", "games", "游戏中心", true, false, "&#127918;", 8));

        // 教师权限：可管理学生、教师、仓库；不能提交投诉
        rolePermissionRepository.save(new RolePermission("TEACHER", "users", "用户管理", false, false, "&#128100;", 1));
        rolePermissionRepository.save(new RolePermission("TEACHER", "students", "学生管理", true, true, "&#127891;", 2));
        rolePermissionRepository.save(new RolePermission("TEACHER", "teachers", "教师管理", true, true, "&#128221;", 3));
        rolePermissionRepository.save(new RolePermission("TEACHER", "inventory", "仓库管理", true, true, "&#128230;", 4));
        rolePermissionRepository.save(new RolePermission("TEACHER", "complaints", "投诉反馈", false, false, "&#128172;", 5));
        rolePermissionRepository.save(new RolePermission("TEACHER", "complaintManage", "投诉管理", false, false, "&#128203;", 6));
        rolePermissionRepository.save(new RolePermission("TEACHER", "roles", "角色权限", true, false, "&#128275;", 7));
        rolePermissionRepository.save(new RolePermission("TEACHER", "games", "游戏中心", true, false, "&#127918;", 8));

        // 学生权限：只能提交投诉反馈；可查看角色权限页面（只读）
        rolePermissionRepository.save(new RolePermission("STUDENT", "users", "用户管理", false, false, "&#128100;", 1));
        rolePermissionRepository.save(new RolePermission("STUDENT", "students", "学生管理", false, false, "&#127891;", 2));
        rolePermissionRepository.save(new RolePermission("STUDENT", "teachers", "教师管理", false, false, "&#128221;", 3));
        rolePermissionRepository.save(new RolePermission("STUDENT", "inventory", "仓库管理", false, false, "&#128230;", 4));
        rolePermissionRepository.save(new RolePermission("STUDENT", "complaints", "投诉反馈", true, true, "&#128172;", 5));
        rolePermissionRepository.save(new RolePermission("STUDENT", "complaintManage", "投诉管理", false, false, "&#128203;", 6));
        rolePermissionRepository.save(new RolePermission("STUDENT", "roles", "角色权限", true, false, "&#128275;", 7));
        rolePermissionRepository.save(new RolePermission("STUDENT", "games", "游戏中心", true, false, "&#127918;", 8));

        logger.info("初始化默认角色权限数据成功");
        result.put("success", true);
        result.put("message", "初始化成功");
        return ResponseEntity.ok(result);
    }
}
