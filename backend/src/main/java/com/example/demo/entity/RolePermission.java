package com.example.demo.entity;

import jakarta.persistence.*;

/**
 * 角色权限实体类。
 * <p>
 * 对应数据库表 {@code role_permissions}，实现 RBAC（基于角色的访问控制）模型。
 * </p>
 * <p>
 * 设计说明：
 * <ul>
 *   <li>每行记录代表某个角色对某个模块的具体权限配置</li>
 *   <li>roleName：角色标识（ADMIN / STUDENT / TEACHER）</li>
 *   <li>moduleKey：模块键值（如 users、students、inventory），前端路由与菜单匹配用</li>
 *   <li>moduleLabel：模块显示名称（如"用户管理"），用于前端菜单展示</li>
 *   <li>canView：是否有查看/访问权限</li>
 *   <li>canEdit：是否有编辑/操作权限</li>
 *   <li>icon：菜单图标（HTML 实体编码，如 &#128100;）</li>
 *   <li>sortOrder：菜单排序顺序，越小越靠前</li>
 * </ul>
 * </p>
 */
@Entity
@Table(name = "role_permissions")
public class RolePermission {

    /** 权限主键ID，自增 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 角色名称（ADMIN / STUDENT / TEACHER） */
    @Column(name = "role_name", nullable = false)
    private String roleName;

    /** 模块键值（前端路由标识） */
    @Column(name = "module_key", nullable = false)
    private String moduleKey;

    /** 模块显示标签（菜单名称） */
    @Column(name = "module_label", nullable = false)
    private String moduleLabel;

    /** 是否有查看权限 */
    @Column(name = "can_view", nullable = false)
    private Boolean canView = false;

    /** 是否有编辑权限 */
    @Column(name = "can_edit", nullable = false)
    private Boolean canEdit = false;

    /** 菜单图标（HTML Unicode 实体） */
    @Column(name = "icon")
    private String icon;

    /** 排序顺序，用于控制菜单展示顺序 */
    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;

    public RolePermission() {
    }

    public RolePermission(String roleName, String moduleKey, String moduleLabel, Boolean canView, Boolean canEdit, String icon, Integer sortOrder) {
        this.roleName = roleName;
        this.moduleKey = moduleKey;
        this.moduleLabel = moduleLabel;
        this.canView = canView;
        this.canEdit = canEdit;
        this.icon = icon;
        this.sortOrder = sortOrder;
    }

    // ==================== Getter / Setter ====================

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getRoleName() {
        return roleName;
    }

    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }

    public String getModuleKey() {
        return moduleKey;
    }

    public void setModuleKey(String moduleKey) {
        this.moduleKey = moduleKey;
    }

    public String getModuleLabel() {
        return moduleLabel;
    }

    public void setModuleLabel(String moduleLabel) {
        this.moduleLabel = moduleLabel;
    }

    public Boolean getCanView() {
        return canView;
    }

    public void setCanView(Boolean canView) {
        this.canView = canView;
    }

    public Boolean getCanEdit() {
        return canEdit;
    }

    public void setCanEdit(Boolean canEdit) {
        this.canEdit = canEdit;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }
}
