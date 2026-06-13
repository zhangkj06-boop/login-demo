package com.example.demo.repository;

import com.example.demo.entity.Complaint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 投诉信息数据访问接口。
 * <p>
 * 继承 {@link JpaRepository}，提供投诉记录的增删改查及排序查询能力。
 * </p>
 */
@Repository
public interface ComplaintRepository extends JpaRepository<Complaint, Long> {

    /**
     * 根据提交人姓名查询投诉记录，按创建时间降序排列。
     * <p>
     * 用于"我的投诉"页面，展示当前登录学生提交的所有投诉。
     * </p>
     *
     * @param submitterName 提交人姓名
     * @return 投诉记录列表（最新的在前）
     */
    List<Complaint> findBySubmitterNameOrderByCreatedAtDesc(String submitterName);
}
