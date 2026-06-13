package com.example.demo.repository;

import com.example.demo.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 用户信息数据访问接口。
 * <p>
 * 继承 {@link JpaRepository}，提供管理员用户的基础 CRUD 及自定义查询支持。
 * </p>
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * 根据用户名查询用户信息。
     * <p>
     * 用于管理员登录时的身份校验。
     * </p>
     *
     * @param username 用户名
     * @return 用户信息 Optional 包装
     */
    Optional<User> findByUsername(String username);

    /**
     * 判断是否存在指定用户名的用户。
     * <p>
     * 用于注册或创建用户时的用户名唯一性校验。
     * </p>
     *
     * @param username 用户名
     * @return true 表示已存在
     */
    boolean existsByUsername(String username);
}
