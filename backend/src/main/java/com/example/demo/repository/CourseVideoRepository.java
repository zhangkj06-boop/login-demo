package com.example.demo.repository;

import com.example.demo.entity.CourseVideo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 微课视频数据访问接口。
 */
@Repository
public interface CourseVideoRepository extends JpaRepository<CourseVideo, Long> {

    /**
     * 按创建时间降序查询所有微课视频。
     */
    List<CourseVideo> findAllByOrderByCreateTimeDesc();

    /**
     * 按状态查询微课视频列表。
     */
    List<CourseVideo> findByStatusOrderByCreateTimeDesc(String status);
}
