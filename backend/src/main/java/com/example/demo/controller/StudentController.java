package com.example.demo.controller;

import com.example.demo.entity.Student;
import com.example.demo.repository.StudentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 学生控制器。
 * <p>
 * 提供学生管理的完整 REST API，包括增删改查。
 * </p>
 * <p>
 * 后端校验规则：
 * <ul>
 *   <li>姓名、学号、性别、班级、密码为必填</li>
 *   <li>学号全局唯一</li>
 *   <li>年龄必须在 0-150 之间</li>
 *   <li>密码长度不少于 6 位，存储前经 BCrypt 加密</li>
 * </ul>
 * </p>
 */
@RestController
@RequestMapping("/api/students")
@CrossOrigin(origins = "*")
public class StudentController {

    /** 日志记录器 */
    private static final Logger logger = LoggerFactory.getLogger(StudentController.class);

    /** 学生数据访问接口 */
    private final StudentRepository studentRepository;
    /** 密码加密器 */
    private final BCryptPasswordEncoder passwordEncoder;

    public StudentController(StudentRepository studentRepository, BCryptPasswordEncoder passwordEncoder) {
        this.studentRepository = studentRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * 查询所有学生列表。
     *
     * @return 学生列表
     */
    @GetMapping
    public ResponseEntity<List<Student>> listStudents() {
        logger.info("查询所有学生列表");
        List<Student> students = studentRepository.findAll();
        logger.info("查询学生列表成功，共 {} 条记录", students.size());
        return ResponseEntity.ok(students);
    }

    /**
     * 根据ID查询学生详情。
     *
     * @param id 学生主键
     * @return 学生详情；不存在返回 404
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getStudent(@PathVariable Long id) {
        logger.info("查询学生详情，ID: {}", id);
        return studentRepository.findById(id)
                .map(student -> {
                    logger.info("查询学生详情成功，ID: {}", id);
                    return ResponseEntity.ok(student);
                })
                .orElseGet(() -> {
                    logger.warn("查询学生详情失败，学生不存在，ID: {}", id);
                    return ResponseEntity.status(404).body((Student) null);
                });
    }

    /**
     * 创建新学生。
     *
     * @param student 学生实体
     * @return 创建结果
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> createStudent(@RequestBody Student student) {
        logger.info("创建学生，姓名: {}, 学生ID: {}", student.getName(), student.getStudentId());
        Map<String, Object> result = new HashMap<>();
        if (student.getName() == null || student.getName().isBlank()) {
            logger.warn("创建学生失败，姓名不能为空");
            result.put("success", false);
            result.put("message", "姓名不能为空");
            return ResponseEntity.badRequest().body(result);
        }
        if (student.getStudentId() == null || student.getStudentId().isBlank()) {
            logger.warn("创建学生失败，学生ID不能为空");
            result.put("success", false);
            result.put("message", "学生ID不能为空");
            return ResponseEntity.badRequest().body(result);
        }
        if (studentRepository.existsByStudentId(student.getStudentId())) {
            logger.warn("创建学生失败，学生ID已存在: {}", student.getStudentId());
            result.put("success", false);
            result.put("message", "学生ID已存在");
            return ResponseEntity.badRequest().body(result);
        }
        if (student.getAge() == null || student.getAge() < 0 || student.getAge() > 150) {
            logger.warn("创建学生失败，年龄必须在0-150之间");
            result.put("success", false);
            result.put("message", "年龄必须在0-150之间");
            return ResponseEntity.badRequest().body(result);
        }
        if (student.getGender() == null || student.getGender().isBlank()) {
            logger.warn("创建学生失败，性别不能为空");
            result.put("success", false);
            result.put("message", "性别不能为空");
            return ResponseEntity.badRequest().body(result);
        }
        if (student.getClassName() == null || student.getClassName().isBlank()) {
            logger.warn("创建学生失败，班级不能为空");
            result.put("success", false);
            result.put("message", "班级不能为空");
            return ResponseEntity.badRequest().body(result);
        }
        if (student.getPassword() == null || student.getPassword().isBlank()) {
            logger.warn("创建学生失败，密码不能为空");
            result.put("success", false);
            result.put("message", "密码不能为空");
            return ResponseEntity.badRequest().body(result);
        }
        if (student.getPassword().length() < 6) {
            logger.warn("创建学生失败，密码长度不能少于6位");
            result.put("success", false);
            result.put("message", "密码长度不能少于6位");
            return ResponseEntity.badRequest().body(result);
        }
        student.setPassword(passwordEncoder.encode(student.getPassword()));
        Student saved = studentRepository.save(student);
        logger.info("创建学生成功，学生ID: {}", saved.getStudentId());
        result.put("success", true);
        result.put("message", "创建成功");
        result.put("id", saved.getId());
        return ResponseEntity.ok(result);
    }

    /**
     * 更新学生信息（部分更新）。
     *
     * @param id      学生主键
     * @param student 更新的内容
     * @return 更新结果
     */
    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateStudent(@PathVariable Long id, @RequestBody Student student) {
        logger.info("更新学生，ID: {}", id);
        Map<String, Object> result = new HashMap<>();
        Student existing = studentRepository.findById(id).orElse(null);
        if (existing == null) {
            logger.warn("更新学生失败，学生不存在，ID: {}", id);
            result.put("success", false);
            result.put("message", "学生不存在");
            return ResponseEntity.status(404).body(result);
        }
        if (student.getName() != null && !student.getName().isBlank()) {
            existing.setName(student.getName());
        }
        if (student.getStudentId() != null && !student.getStudentId().isBlank()
                && !student.getStudentId().equals(existing.getStudentId())) {
            if (studentRepository.existsByStudentId(student.getStudentId())) {
                logger.warn("更新学生失败，学生ID已存在: {}", student.getStudentId());
                result.put("success", false);
                result.put("message", "学生ID已存在");
                return ResponseEntity.badRequest().body(result);
            }
            existing.setStudentId(student.getStudentId());
        }
        if (student.getAge() != null) {
            if (student.getAge() < 0 || student.getAge() > 150) {
                logger.warn("更新学生失败，年龄必须在0-150之间");
                result.put("success", false);
                result.put("message", "年龄必须在0-150之间");
                return ResponseEntity.badRequest().body(result);
            }
            existing.setAge(student.getAge());
        }
        if (student.getGender() != null && !student.getGender().isBlank()) {
            existing.setGender(student.getGender());
        }
        if (student.getClassName() != null && !student.getClassName().isBlank()) {
            existing.setClassName(student.getClassName());
        }
        if (student.getPassword() != null && !student.getPassword().isBlank()) {
            if (student.getPassword().length() < 6) {
                logger.warn("更新学生失败，密码长度不能少于6位");
                result.put("success", false);
                result.put("message", "密码长度不能少于6位");
                return ResponseEntity.badRequest().body(result);
            }
            existing.setPassword(passwordEncoder.encode(student.getPassword()));
        }
        studentRepository.save(existing);
        logger.info("更新学生成功，ID: {}", id);
        result.put("success", true);
        result.put("message", "更新成功");
        return ResponseEntity.ok(result);
    }

    /**
     * 删除学生。
     *
     * @param id 学生主键
     * @return 删除结果
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteStudent(@PathVariable Long id) {
        logger.info("删除学生，ID: {}", id);
        Map<String, Object> result = new HashMap<>();
        if (!studentRepository.existsById(id)) {
            logger.warn("删除学生失败，学生不存在，ID: {}", id);
            result.put("success", false);
            result.put("message", "学生不存在");
            return ResponseEntity.status(404).body(result);
        }
        studentRepository.deleteById(id);
        logger.info("删除学生成功，ID: {}", id);
        result.put("success", true);
        result.put("message", "删除成功");
        return ResponseEntity.ok(result);
    }
}
