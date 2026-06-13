package com.example.demo.entity;

import jakarta.persistence.*;

import java.util.List;

/**
 * 教师实体类。
 * <p>
 * 对应数据库表 {@code teachers}，存储教师基本信息、授课班级及登录凭证。
 * </p>
 * <p>
 * 特殊字段说明：
 * <ul>
 *   <li>teacherId：教师工号，全局唯一</li>
 *   <li>classes：授课班级列表，使用 {@code @ElementCollection} 映射到关联表 {@code teacher_classes}</li>
 *   <li>ClassEnum：班级枚举，限制可选班级范围（高二1班/高二2班/高二3班）</li>
 *   <li>{@code @Enumerated(EnumType.STRING)}：枚举在数据库中存储为字符串（而非序号），提高可读性和稳定性</li>
 * </ul>
 * </p>
 */
@Entity
@Table(name = "teachers")
public class Teacher {

    /** 教师主键ID，自增 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 教师工号（全局唯一） */
    @Column(name = "teacher_id", unique = true, nullable = false)
    private String teacherId;

    /** 教师姓名 */
    @Column(nullable = false)
    private String name;

    /** 教师年龄 */
    @Column(nullable = false)
    private Integer age;

    /** 教师性别 */
    @Column(nullable = false, length = 10)
    private String gender;

    /** 教师职位/职称（如教授、副教授、讲师） */
    @Column(nullable = false)
    private String position;

    /**
     * 授课班级列表。
     * <p>
     * {@code @ElementCollection}：表示这是一个由基本类型或枚举组成的集合，
     * JPA 会自动创建关联表 {@code teacher_classes} 存储班级数据。
     * </p>
     * <p>
     * {@code fetch = FetchType.EAGER}：立即加载，避免延迟加载导致的会话关闭问题（当前项目未开启 OpenEntityManagerInView）。
     * </p>
     */
    @ElementCollection(targetClass = ClassEnum.class, fetch = FetchType.EAGER)
    @CollectionTable(name = "teacher_classes", joinColumns = @JoinColumn(name = "teacher_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "class_name")
    private List<ClassEnum> classes;

    /** 登录密码（BCrypt 哈希存储） */
    @Column(nullable = false)
    private String password;

    public Teacher() {
    }

    public Teacher(String teacherId, String name, Integer age, String gender, String position, List<ClassEnum> classes) {
        this.teacherId = teacherId;
        this.name = name;
        this.age = age;
        this.gender = gender;
        this.position = position;
        this.classes = classes;
    }

    // ==================== Getter / Setter ====================

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTeacherId() {
        return teacherId;
    }

    public void setTeacherId(String teacherId) {
        this.teacherId = teacherId;
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

    public String getPosition() {
        return position;
    }

    public void setPosition(String position) {
        this.position = position;
    }

    public List<ClassEnum> getClasses() {
        return classes;
    }

    public void setClasses(List<ClassEnum> classes) {
        this.classes = classes;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
