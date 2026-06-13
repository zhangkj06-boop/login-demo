package com.example.demo.repository;

import com.example.demo.entity.Teacher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 教师信息数据访问接口。
 * <p>
 * 继承 {@link JpaRepository}，提供教师信息的基础 CRUD 及自定义查询支持。
 * </p>
 */
@Repository
public interface TeacherRepository extends JpaRepository<Teacher, Long> {

    /**
     * 根据教师编号查询教师信息。
     * <p>
     * 用于教师登录时的身份校验（教师ID作为登录账号）。
     * </p>
     *
     * @param teacherId 教师编号
     * @return 教师信息 Optional 包装
     */
    Optional<Teacher> findByTeacherId(String teacherId);

    /**
     * 判断是否存在指定教师编号的教师。
     * <p>
     * 用于创建教师时的工号唯一性校验。
     * </p>
     *
     * @param teacherId 教师编号
     * @return true 表示已存在
     */
    boolean existsByTeacherId(String teacherId);

    /**
     * 根据姓名查询教师列表。
     * <p>
     * 用于支持教师通过姓名登录（作为教师ID登录的备选方式）。
     * </p>
     *
     * @param name 姓名
     * @return 教师列表
     */
    List<Teacher> findByName(String name);
}
