package com.example.demo.repository;

import com.example.demo.entity.Game;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 游戏数据访问接口。
 * <p>
 * 继承 {@link JpaRepository}，提供游戏信息的基础 CRUD 及自定义查询支持。
 * </p>
 */
@Repository
public interface GameRepository extends JpaRepository<Game, Long> {

    /**
     * 根据游戏编码查询游戏。
     *
     * @param code 游戏编码
     * @return 游戏信息 Optional 包装
     */
    Optional<Game> findByCode(String code);

    /**
     * 判断是否存在指定编码的游戏。
     *
     * @param code 游戏编码
     * @return true 表示已存在
     */
    boolean existsByCode(String code);

    /**
     * 查询所有已上线的游戏，按排序号升序排列。
     *
     * @param status 游戏状态
     * @return 游戏列表
     */
    List<Game> findByStatusOrderBySortOrderAsc(String status);

    /**
     * 查询所有游戏，按排序号升序排列。
     *
     * @return 游戏列表
     */
    List<Game> findAllByOrderBySortOrderAsc();
}
