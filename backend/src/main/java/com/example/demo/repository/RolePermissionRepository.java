package com.example.demo.repository;

import com.example.demo.entity.RolePermission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 角色权限数据访问接口。
 * <p>
 * 继承 {@link JpaRepository}，提供角色权限的基础 CRUD 及自定义查询支持。
 * </p>
 */
@Repository
public interface RolePermissionRepository extends JpaRepository<RolePermission, Long> {

    /**
     * 根据角色名称查询权限列表，按排序顺序升序排列。
     * <p>
     * 用于前端登录成功后获取当前角色的导航菜单权限，
     * sortOrder 升序保证菜单按预设顺序展示。
     * </p>
     *
     * @param roleName 角色名称（ADMIN / STUDENT / TEACHER）
     * @return 该角色的权限配置列表
     */
    List<RolePermission> findByRoleNameOrderBySortOrderAsc(String roleName);
}
