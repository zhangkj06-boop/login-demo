package com.example.demo.entity;

import jakarta.persistence.*;

/**
 * 学生实体类。
 * <p>
 * 对应数据库表 {@code students}，存储学生基本信息及登录凭证。
 * </p>
 * <p>
 * 后端校验规则：
 * <ul>
 *   <li>姓名、年龄、性别、学号、班级、密码均为必填</li>
 *   <li>student_id 全局唯一（业务上学号不可重复）</li>
 *   <li>年龄需在 0-150 之间</li>
 *   <li>密码长度不少于 6 位，存储前经 BCrypt 加密</li>
 * </ul>
 * </p>
 */
@Entity
@Table(name = "students")
public class Student {

    /** 学生主键ID，自增 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 学生姓名 */
    @Column(nullable = false)
    private String name;

    /** 学生年龄 */
    @Column(nullable = false)
    private Integer age;

    /** 学生性别 */
    @Column(nullable = false, length = 10)
    private String gender;

    /** 学号（全局唯一标识） */
    @Column(name = "student_id", unique = true, nullable = false)
    private String studentId;

    /** 班级名称 */
    @Column(nullable = false)
    private String className;

    /** 登录密码（BCrypt 哈希存储，非明文） */
    @Column(nullable = false)
    private String password;

    public Student() {
    }

    public Student(String name, Integer age, String gender, String studentId, String className) {
        this.name = name;
        this.age = age;
        this.gender = gender;
        this.studentId = studentId;
        this.className = className;
    }

    // ==================== Getter / Setter ====================

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getAge() {
        return age;
    }

    public void setAge(Integer age) {
        this.age = age;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getStudentId() {
        return studentId;
    }

    public void setStudentId(String studentId) {
        this.studentId = studentId;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
