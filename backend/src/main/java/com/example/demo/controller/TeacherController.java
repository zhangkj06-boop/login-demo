package com.example.demo.controller;

import com.example.demo.entity.Teacher;
import com.example.demo.repository.TeacherRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 教师控制器。
 * <p>
 * 提供教师管理的完整 REST API，包括增删改查。
 * </p>
 * <p>
 * 与 {@link StudentController} 的区别：
 * <ul>
 *   <li>教师使用 teacherId（工号）作为唯一标识，而非自增 id</li>
 *   <li>教师支持 position（职称）和 classes（授课班级列表）字段</li>
 *   <li>classes 通过 {@code @ElementCollection} 映射到 teacher_classes 关联表</li>
 * </ul>
 * </p>
 */
@RestController
@RequestMapping("/api/teachers")
@CrossOrigin(origins = "*")
public class TeacherController {

    /** 日志记录器 */
    private static final Logger logger = LoggerFactory.getLogger(TeacherController.class);

    /** 教师数据访问接口 */
    private final TeacherRepository teacherRepository;
    /** 密码加密器 */
    private final BCryptPasswordEncoder passwordEncoder;

    public TeacherController(TeacherRepository teacherRepository, BCryptPasswordEncoder passwordEncoder) {
        this.teacherRepository = teacherRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * 查询所有教师列表。
     *
     * @return 教师列表（包含授课班级 classes）
     */
    @GetMapping
    public ResponseEntity<List<Teacher>> listTeachers() {
        logger.info("查询所有教师列表");
        List<Teacher> teachers = teacherRepository.findAll();
        logger.info("查询教师列表成功，共 {} 条记录", teachers.size());
        return ResponseEntity.ok(teachers);
    }

    /**
     * 根据ID查询教师详情。
     *
     * @param id 教师主键
     * @return 教师详情；不存在返回 404
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getTeacher(@PathVariable Long id) {
        logger.info("查询教师详情，ID: {}", id);
        return teacherRepository.findById(id)
                .map(teacher -> {
                    logger.info("查询教师详情成功，ID: {}", id);
                    return ResponseEntity.ok(teacher);
                })
                .orElseGet(() -> {
                    logger.warn("查询教师详情失败，教师不存在，ID: {}", id);
                    return ResponseEntity.status(404).body((Teacher) null);
                });
    }

    /**
     * 创建新教师。
     *
     * @param teacher 教师实体
     * @return 创建结果
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> createTeacher(@RequestBody Teacher teacher) {
        logger.info("创建教师，姓名: {}, 教师ID: {}", teacher.getName(), teacher.getTeacherId());
        Map<String, Object> result = new HashMap<>();
        if (teacher.getName() == null || teacher.getName().isBlank()) {
            logger.warn("创建教师失败，教师名称不能为空");
            result.put("success", false);
            result.put("message", "教师名称不能为空");
            return ResponseEntity.badRequest().body(result);
        }
        if (teacher.getTeacherId() == null || teacher.getTeacherId().isBlank()) {
            logger.warn("创建教师失败，教师ID不能为空");
            result.put("success", false);
            result.put("message", "教师ID不能为空");
            return ResponseEntity.badRequest().body(result);
        }
        if (teacherRepository.existsByTeacherId(teacher.getTeacherId())) {
            logger.warn("创建教师失败，教师ID已存在: {}", teacher.getTeacherId());
            result.put("success", false);
            result.put("message", "教师ID已存在");
            return ResponseEntity.badRequest().body(result);
        }
        if (teacher.getAge() == null || teacher.getAge() < 0 || teacher.getAge() > 150) {
            logger.warn("创建教师失败，年龄必须在0-150之间");
            result.put("success", false);
            result.put("message", "年龄必须在0-150之间");
            return ResponseEntity.badRequest().body(result);
        }
        if (teacher.getGender() == null || teacher.getGender().isBlank()) {
            logger.warn("创建教师失败，性别不能为空");
            result.put("success", false);
            result.put("message", "性别不能为空");
            return ResponseEntity.badRequest().body(result);
        }
        if (teacher.getPosition() == null || teacher.getPosition().isBlank()) {
            logger.warn("创建教师失败，岗位不能为空");
            result.put("success", false);
            result.put("message", "岗位不能为空");
            return ResponseEntity.badRequest().body(result);
        }
        if (teacher.getClasses() == null || teacher.getClasses().isEmpty()) {
            logger.warn("创建教师失败，至少选择一个班级");
            result.put("success", false);
            result.put("message", "至少选择一个班级");
            return ResponseEntity.badRequest().body(result);
        }
        if (teacher.getPassword() == null || teacher.getPassword().isBlank()) {
            logger.warn("创建教师失败，密码不能为空");
            result.put("success", false);
            result.put("message", "密码不能为空");
            return ResponseEntity.badRequest().body(result);
        }
        if (teacher.getPassword().length() < 6) {
            logger.warn("创建教师失败，密码长度不能少于6位");
            result.put("success", false);
            result.put("message", "密码长度不能少于6位");
            return ResponseEntity.badRequest().body(result);
        }
        teacher.setPassword(passwordEncoder.encode(teacher.getPassword()));
        Teacher saved = teacherRepository.save(teacher);
        logger.info("创建教师成功，教师ID: {}", saved.getTeacherId());
        result.put("success", true);
        result.put("message", "创建成功");
        result.put("id", saved.getId());
        return ResponseEntity.ok(result);
    }

    /**
     * 更新教师信息（部分更新）。
     *
     * @param id      教师主键
     * @param teacher 更新的内容
     * @return 更新结果
     */
    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateTeacher(@PathVariable Long id, @RequestBody Teacher teacher) {
        logger.info("更新教师，ID: {}", id);
        Map<String, Object> result = new HashMap<>();
        Teacher existing = teacherRepository.findById(id).orElse(null);
        if (existing == null) {
            logger.warn("更新教师失败，教师不存在，ID: {}", id);
            result.put("success", false);
            result.put("message", "教师不存在");
            return ResponseEntity.status(404).body(result);
        }
        if (teacher.getName() != null && !teacher.getName().isBlank()) {
            existing.setName(teacher.getName());
        }
        if (teacher.getTeacherId() != null && !teacher.getTeacherId().isBlank()
                && !teacher.getTeacherId().equals(existing.getTeacherId())) {
            if (teacherRepository.existsByTeacherId(teacher.getTeacherId())) {
                logger.warn("更新教师失败，教师ID已存在: {}", teacher.getTeacherId());
                result.put("success", false);
                result.put("message", "教师ID已存在");
                return ResponseEntity.badRequest().body(result);
            }
            existing.setTeacherId(teacher.getTeacherId());
        }
        if (teacher.getAge() != null) {
            if (teacher.getAge() < 0 || teacher.getAge() > 150) {
                logger.warn("更新教师失败，年龄必须在0-150之间");
                result.put("success", false);
                result.put("message", "年龄必须在0-150之间");
                return ResponseEntity.badRequest().body(result);
            }
            existing.setAge(teacher.getAge());
        }
        if (teacher.getGender() != null && !teacher.getGender().isBlank()) {
            existing.setGender(teacher.getGender());
        }
        if (teacher.getPosition() != null && !teacher.getPosition().isBlank()) {
            existing.setPosition(teacher.getPosition());
        }
        if (teacher.getClasses() != null && !teacher.getClasses().isEmpty()) {
            existing.setClasses(teacher.getClasses());
        }
        if (teacher.getPassword() != null && !teacher.getPassword().isBlank()) {
            if (teacher.getPassword().length() < 6) {
                logger.warn("更新教师失败，密码长度不能少于6位");
                result.put("success", false);
                result.put("message", "密码长度不能少于6位");
                return ResponseEntity.badRequest().body(result);
            }
            existing.setPassword(passwordEncoder.encode(teacher.getPassword()));
        }
        teacherRepository.save(existing);
        logger.info("更新教师成功，ID: {}", id);
        result.put("success", true);
        result.put("message", "更新成功");
        return ResponseEntity.ok(result);
    }

    /**
     * 删除教师。
     *
     * @param id 教师主键
     * @return 删除结果
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteTeacher(@PathVariable Long id) {
        logger.info("删除教师，ID: {}", id);
        Map<String, Object> result = new HashMap<>();
        if (!teacherRepository.existsById(id)) {
            logger.warn("删除教师失败，教师不存在，ID: {}", id);
            result.put("success", false);
            result.put("message", "教师不存在");
            return ResponseEntity.status(404).body(result);
        }
        teacherRepository.deleteById(id);
        logger.info("删除教师成功，ID: {}", id);
        result.put("success", true);
        result.put("message", "删除成功");
        return ResponseEntity.ok(result);
    }
}
