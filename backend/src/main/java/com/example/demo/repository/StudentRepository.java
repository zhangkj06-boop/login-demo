package com.example.demo.repository;

import com.example.demo.entity.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 学生信息数据访问接口。
 * <p>
 * 继承 {@link JpaRepository}，提供学生信息的基础 CRUD 及自定义查询支持。
 * </p>
 */
@Repository
public interface StudentRepository extends JpaRepository<Student, Long> {

    /**
     * 根据学号查询学生信息。
     * <p>
     * 用于学生登录时的身份校验（学号作为登录账号）。
     * </p>
     *
     * @param studentId 学号
     * @return 学生信息 Optional 包装（避免 null 返回）
     */
    Optional<Student> findByStudentId(String studentId);

    /**
     * 判断是否存在指定学号的学生。
     * <p>
     * 用于创建学生时的学号唯一性校验。
     * </p>
     *
     * @param studentId 学号
     * @return true 表示已存在
     */
    boolean existsByStudentId(String studentId);

    /**
     * 根据姓名查询学生列表。
     * <p>
     * 用于支持学生通过姓名登录（作为学号登录的备选方式）。
     * </p>
     *
     * @param name 姓名
     * @return 学生列表（可能有多人同名）
     */
    List<Student> findByName(String name);
}
