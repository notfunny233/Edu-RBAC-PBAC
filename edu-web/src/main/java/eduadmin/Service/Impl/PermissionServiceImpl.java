package eduadmin.Service.Impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import eduadmin.Exception.BusinessException;
import eduadmin.Mapper.PermissionMapper;
import eduadmin.Mapper.RolePermissionMapper;
import eduadmin.Service.PermissionService;
import eduadmin.pojo.PageResult;
import eduadmin.pojo.Permission;
import eduadmin.pojo.PermissionQueryParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service//交给IOC容器管理
public class PermissionServiceImpl implements PermissionService {

    /** 通配权限点编码，删除保护要用 */
    private static final String ALL_CODE = "*";

    @Autowired
    private PermissionMapper permissionMapper;
    @Autowired
    private RolePermissionMapper rolePermissionMapper;

    @Override
    public PageResult<Permission> page(PermissionQueryParam permissionQueryParam) {
        //设置分页参数
        PageHelper.startPage(permissionQueryParam.getPage(), permissionQueryParam.getPageSize());

        //调用Mapper接口
        List<Permission> rows = permissionMapper.list(permissionQueryParam);

        //封装成对象
        PageInfo<Permission> pageInfo = new PageInfo<>(rows);
        return new PageResult<>(pageInfo.getTotal(), pageInfo.getList());
    }

    @Transactional(rollbackFor = {Exception.class})
    @Override
    public void save(Permission permission) {
        //1. 业务校验：权限点编码唯一
        if (permissionMapper.countByCodeExcludeId(permission.getCode(), 0) > 0) {
            throw new BusinessException("权限点编码 " + permission.getCode() + " 已存在，请换一个");
        }
        //2. 业务校验：编码格式约定 resource:action，或通配的 *
        //   格式错了不会立刻报错，而是"授权了但接口永远校验不通过"，
        //   这种问题排查起来比直接拒绝麻烦得多，所以在入口就拦住
        if (!ALL_CODE.equals(permission.getCode())
                && !permission.getCode().contains(":")) {
            throw new BusinessException("权限点编码需符合 resource:action 格式，例如 emp:delete");
        }
        //3. 补全基础属性 - createTime, updateTime
        permission.setCreateTime(LocalDateTime.now());
        permission.setUpdateTime(LocalDateTime.now());
        //4. 调用Mapper接口方法插入数据
        permissionMapper.insert(permission);
    }

    /**
     * 批量删除权限点
     * 两张表的写操作，必须用事务包住，理由同 RoleServiceImpl.del
     */
    @Transactional(rollbackFor = {Exception.class})
    @Override
    public void del(List<Integer> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        //1. 保护性校验：通配权限点 * 不能删
        //   它被删掉，持有它的超级管理员就失去全部权限，同样是"要改数据库才能救回来"的事故
        List<String> codes = permissionMapper.selectCodesByIds(ids);
        if (codes != null && codes.contains(ALL_CODE)) {
            throw new BusinessException("通配权限点 * 不允许删除，它是超级管理员的权限来源");
        }
        //2. 先清关联表，避免 role_permission 留下指向已删权限点的悬空行
        rolePermissionMapper.deleteByPermissionIds(ids);
        //3. 再删权限点本体
        permissionMapper.del(ids);
    }

    @Override
    public Permission getById(Integer id) {
        Permission permission = permissionMapper.getById(id);
        return permission;
    }

    @Transactional(rollbackFor = {Exception.class})
    @Override
    public void update(Permission permission) {
        //1. 业务校验：编码唯一，排除自己
        if (permissionMapper.countByCodeExcludeId(permission.getCode(), permission.getId()) > 0) {
            throw new BusinessException("权限点编码 " + permission.getCode() + " 已被其他权限点占用");
        }
        //2. 业务校验：编码格式
        if (!ALL_CODE.equals(permission.getCode())
                && !permission.getCode().contains(":")) {
            throw new BusinessException("权限点编码需符合 resource:action 格式，例如 emp:delete");
        }
        //3. 补全基础属性 - updateTime
        permission.setUpdateTime(LocalDateTime.now());
        //4. 调用Mapper接口方法更新数据
        permissionMapper.update(permission);
    }

    @Override
    public List<Permission> findall() {
        return permissionMapper.findall();
    }

}
