package eduadmin.Service.Impl;

import eduadmin.Exception.BusinessException;
import eduadmin.Mapper.EmpRoleMapper;
import eduadmin.Mapper.PermissionMapper;
import eduadmin.Mapper.RolePermissionMapper;
import eduadmin.Service.AuthService;
import eduadmin.pojo.Emp;
import eduadmin.pojo.Permission;
import eduadmin.utils.LoginContextHolder;
import eduadmin.utils.PermissionCache;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
@Service//交给IOC容器管理
public class AuthServiceImpl implements AuthService {

    /** 通配权限点，含义与 PolicyDecisionPoint 里保持一致 */
    private static final String ALL_CODE = "*";

    @Autowired
    private EmpRoleMapper empRoleMapper;
    @Autowired
    private RolePermissionMapper rolePermissionMapper;
    @Autowired
    private PermissionMapper permissionMapper;
    @Autowired
    private PermissionCache permissionCache;

    /**
     * 查询当前登录人的权限点编码集合
     * <p>
     * 这里做了通配展开：如果员工持有 * 权限点，则返回全部具体权限点的编码，
     * 而不是把 * 原样丢给前端。原因是前端只能按编码白名单决定菜单显不显示，
     * 它不认识 * 这个约定；让服务端把"通配"翻译成"具体清单"，
     * 前端代码就不用为超级管理员写一条特殊分支
     */
    @Override
    public List<String> getCurrentPermissionCodes() {
        Emp emp = LoginContextHolder.getEmp();
        if (emp == null) {
            return Collections.emptyList();
        }
        List<Integer> roleIds = permissionCache.loadRoleIds(emp.getId());
        if (roleIds == null || roleIds.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> codes = permissionCache.loadCodes(roleIds);
        if (codes != null && codes.contains(ALL_CODE)) {
            //通配展开：拿全部权限点编码替换 *
            List<Permission> all = permissionMapper.findall();
            List<String> expanded = new ArrayList<>();
            for (Permission permission : all) {
                if (!ALL_CODE.equals(permission.getCode())) {
                    expanded.add(permission.getCode());
                }
            }
            return expanded;
        }
        return codes == null ? Collections.emptyList() : codes;
    }

    @Override
    public List<Integer> getRoleIdsByEmpId(Integer empId) {
        if (empId == null) {
            return Collections.emptyList();
        }
        return empRoleMapper.selectRoleIdsByEmpId(empId);
    }

    /**
     * 给员工分配角色（覆盖式）
     * <p>
     * 两步写操作，必须同成功同失败：如果"清空"成功但"写入"失败，
     * 这个员工会变成没有任何角色的状态 —— 虽然不报错，但他原本的权限全没了，
     * 而且从接口返回上完全看不出来，属于最难发现的那类问题
     */
    @Transactional(rollbackFor = {Exception.class})
    @Override
    public void assignRolesToEmp(Integer empId, List<Integer> roleIds) {
        if (empId == null) {
            throw new BusinessException("员工ID不能为空");
        }
        //1. 清空原有角色关联
        empRoleMapper.deleteByEmpId(empId);
        //2. 写入新角色关联（前端可能把角色全部取消勾选，此时 roleIds 为空，
        //   不判断就会拼出 insert ... values 后面什么都没有的非法SQL）
        if (roleIds != null && !roleIds.isEmpty()) {
            empRoleMapper.insertBatch(empId, roleIds);
        }
        //这个人的角色变了，缓存必须立刻失效，不然他最多 30 分钟内还按老角色鉴权。
        //放在数据库操作之后：就算上面回滚了，最坏也只是缓存里少一条，下次读照样查库补上，不会出错
        permissionCache.evictEmpRoles(empId);
        log.info("给员工[{}]分配角色完成, 角色数:{}", empId, roleIds == null ? 0 : roleIds.size());
    }

    @Override
    public List<Integer> getPermissionIdsByRoleId(Integer roleId) {
        if (roleId == null) {
            return Collections.emptyList();
        }
        return rolePermissionMapper.selectPermissionIdsByRoleId(roleId);
    }

    /**
     * 给角色分配权限点（覆盖式）
     * <p>
     * 这个接口是权限体系里最敏感的一个：改它等于改"谁能做什么"。
     * 所以它自己也要被 role:assign 权限点保护（见 AuthController 上的注解）。
     */
    @Transactional(rollbackFor = {Exception.class})
    @Override
    public void assignPermissionsToRole(Integer roleId, List<Integer> permissionIds) {
        if (roleId == null) {
            throw new BusinessException("角色ID不能为空");
        }
        //1. 清空原有权限点关联
        rolePermissionMapper.deleteByRoleId(roleId);
        //2. 写入新权限点关联
        if (permissionIds != null && !permissionIds.isEmpty()) {
            rolePermissionMapper.insertBatch(roleId, permissionIds);
        }
        //这个角色的权限点变了。删角色的 key 就行：
        //所有持有它的员工下次请求会重新算，不用挨个去清他们的缓存
        permissionCache.evictRoleCodes(roleId);
        log.info("给角色[{}]授权完成, 权限点数:{}", roleId, permissionIds == null ? 0 : permissionIds.size());
    }

}
