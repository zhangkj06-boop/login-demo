package com.example.demo.repository;

import com.example.demo.entity.Announcement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 公告数据访问接口。
 * <p>
 * 继承 {@link JpaRepository}，默认获得常用的 CRUD 和分页方法（save/findAll/findById/delete 等）。
 * </p>
 * <p>
 * Spring Data JPA 会根据方法名自动生成查询逻辑，无需编写 SQL。
 * </p>
 */
@Repository
public interface AnnouncementRepository extends JpaRepository<Announcement, Long> {

    /**
     * 根据发布状态查询公告列表，按创建时间降序排列。
     * <p>
     * 方法名解析：findBy + Status + OrderBy + CreateTime + Desc
     * </p>
     *
     * @param status 发布状态（"草稿" 或 "已发布"）
     * @return 公告列表
     */
    List<Announcement> findByStatusOrderByCreateTimeDesc(String status);

    /**
     * 根据推送对象模糊查询，按创建时间降序排列。
     * <p>
     * Containing 表示 SQL 中的 LIKE %target%
     * </p>
     *
     * @param target 推送对象关键字
     * @return 公告列表
     */
    List<Announcement> findByTargetContainingOrderByCreateTimeDesc(String target);

    /**
     * 查询所有公告，按创建时间降序排列。
     *
     * @return 全部公告列表
     */
    List<Announcement> findAllByOrderByCreateTimeDesc();
}
