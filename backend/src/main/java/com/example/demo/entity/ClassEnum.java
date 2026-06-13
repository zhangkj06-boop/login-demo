package com.example.demo.entity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 班级枚举类。
 * <p>
 * 定义系统中支持的班级选项，用于教师授课班级（{@code classes}）字段的枚举约束。
 * </p>
 * <p>
 * 数据库表 {@code teacher_classes} 中存储的是枚举名称字符串（通过 {@code @Enumerated(EnumType.STRING)} 映射）。
 * </p>
 */
public enum ClassEnum {

    /** 高二1班 */
    高二1班,

    /** 高二2班 */
    高二2班,

    /** 高二3班 */
    高二3班;

    /** 日志记录器（枚举中保留，用于后续可能的业务日志扩展） */
    private static final Logger logger = LoggerFactory.getLogger(ClassEnum.class);
}
