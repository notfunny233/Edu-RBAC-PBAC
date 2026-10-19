package eduadmin.Service.Impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import eduadmin.Exception.BusinessException;
import eduadmin.Mapper.EmpRoleMapper;
import eduadmin.Mapper.RoleMapper;
import eduadmin.Mapper.RolePermissionMapper;
import eduadmin.Service.RoleService;
import eduadmin.pojo.PageResult;
import eduadmin.pojo.Role;
import eduadmin.pojo.RoleQueryParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service//交给IOC容器管理
public class RoleServiceImpl implements RoleService {

    /** 超级管理员角色编码，删除保护要用。抽成常量而不是散在代码里写字符串，避免手滑打错导致保护失效 */
    private static final String SUPER_ADMIN_CODE = "super_admin";

    @Autowired
    private RoleMapper roleMapper;
    @Autowired
    private EmpRoleMapper empRoleMapper;
    @Autowired
    private RolePermissionMapper rolePermissionMapper;

    @Override
    public PageResult<Role> page(RoleQueryParam roleQueryParam) {
        //设置分页参数
        PageHelper.startPage(roleQueryParam.getPage(), roleQueryParam.getPageSize());

        //调用Mapper接口
        List<Role> rows = roleMapper.list(roleQueryParam);

        // 用PageInfo包装，不要强转返回进行响应
        //封装成对象
        PageInfo<Role> pageInfo = new PageInfo<>(rows);
        return new PageResult<>(pageInfo.getTotal(), pageInfo.getList());
    }

    @Transactional(rollbackFor = {Exception.class})
    @Override
    public void save(Role role) {
        //1. 业务校验：角色编码唯一
        //   表上有 uk_role_code 唯一键兜底，这里提前查一次是为了给出人能看懂的提示，
        //   而不是让前端收到一个"服务器异常"（唯一键冲突抛的是 DuplicateKeyException）
        if (roleMapper.countByCodeExcludeId(role.getCode(), 0) > 0) {
            throw new BusinessException("角色编码 " + role.getCode() + " 已存在，请换一个");
        }
        //2. 补全基础属性 - createTime, updateTime
        role.setCreateTime(LocalDateTime.now());
        role.setUpdateTime(LocalDateTime.now());
        //3. 调用Mapper接口方法插入数据
        roleMapper.insert(role);
    }

    /**
     * 批量删除角色
     * <p>
     * 为什么必须加 @Transactional：这个方法要写三张表（emp_role、role_permission、role），
     * 中间任何一步失败都要整体回滚。如果先删了关联、再删角色时失败，
     * 就会留下一批"还在用但已经被清空权限"的角色，这种半成品状态比彻底失败更难收拾。
     * <p>
     * 删除顺序也很关键：必须先删两张关联表，再删主表。
     * 反过来先删角色的话，关联表里就出现了指向不存在角色的悬空行，
     * 下次鉴权时按 role_id 查不到任何东西，表现为"员工突然没有任何权限"，很难定位。
     */
    @Transactional(rollbackFor = {Exception.class})
    @Override
    public void del(List<Integer> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        //1. 保护性校验：超级管理员角色不能删
        //   删掉它意味着 emp 1 失去全部权限，整个后台再没人能进授权页面，
        //   只能手工改数据库才能恢复，属于典型的"点错一下就要重启人生"
        List<String> codes = roleMapper.selectCodesByIds(ids);
        if (codes != null && codes.contains(SUPER_ADMIN_CODE)) {
            throw new BusinessException("超级管理员角色不允许删除，否则将无人能进入授权页面");
        }
        //2. 先清两张关联表（两个方向都要清，漏一边就留悬空引用）
        empRoleMapper.deleteByRoleIds(ids);
        rolePermissionMapper.deleteByRoleIds(ids);
        //3. 再删角色本体
        roleMapper.del(ids);
    }

    @Override
    public Role getById(Integer id) {
        Role role = roleMapper.getById(id);
        return role;
    }

    @Transactional(rollbackFor = {Exception.class})
    @Override
    public void update(Role role) {
        //1. 业务校验：编码唯一，但要排除自己
        //   这里的第二个参数传自己的 id，否则"只改备注不改编码"也会提示编码重复
        if (roleMapper.countByCodeExcludeId(role.getCode(), role.getId()) > 0) {
            throw new BusinessException("角色编码 " + role.getCode() + " 已被其他角色占用");
        }
        //2. 补全基础属性 - updateTime
        role.setUpdateTime(LocalDateTime.now());
        //3. 调用Mapper接口方法更新数据
        roleMapper.update(role);
    }

    @Override
    public List<Role> findall() {
        return roleMapper.findall();
    }

}
