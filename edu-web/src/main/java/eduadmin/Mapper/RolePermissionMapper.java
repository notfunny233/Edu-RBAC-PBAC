package eduadmin.Mapper;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 角色-权限关联 Mapper
 * 同 EmpRoleMapper，关联表不建实体，用 @Param 传参
 */
@Mapper
public interface RolePermissionMapper {

    /**
     * 决策引擎专用：把角色ID集合换成权限点编码集合
     * <p>
     * ⚠️ 调用方必须保证 roleIds 非空。空集合会拼出 in () 这种语法错误的 SQL，
     * 表现为运行时报错而不是"查不到数据"，很容易被误判成别的问题。
     * PolicyDecisionPoint 里已经做了空集合提前返回。
     */
    List<String> selectCodesByRoleIds(@Param("roleIds") List<Integer> roleIds);

    /**
     * 单个角色的权限点编码集合，缓存回填时用。
     * <p>
     * 为什么不用上面那个批量方法：批量查回来的是所有角色混在一起的去重结果，
     * 拿不出"哪个编码属于哪个角色"，没办法按角色往缓存里回填。
     */
    @Select("select distinct p.code from permission p " +
            "inner join role_permission rp on p.id = rp.permission_id " +
            "where rp.role_id = #{roleId}")
    List<String> selectCodesByRoleId(Integer roleId);

    //给角色批量授予权限点（xml 里 foreach 拼批量插入）
    void insertBatch(@Param("roleId") Integer roleId, @Param("permissionIds") List<Integer> permissionIds);

    //查询某角色已有的权限点ID集合，用于授权界面回显"哪些已经勾上了"
    @Select("select permission_id from role_permission where role_id = #{roleId}")
    List<Integer> selectPermissionIdsByRoleId(Integer roleId);

    //清空某角色的全部权限点关联（覆盖式授权用，与员工授权同理）
    @Delete("delete from role_permission where role_id = #{roleId}")
    void deleteByRoleId(Integer roleId);

    //按角色ID批量删除关联，角色被删除时调用（删除角色接口支持一次删多个）
    void deleteByRoleIds(@Param("roleIds") List<Integer> roleIds);

    //按权限点ID批量删除关联，权限点被删除时调用
    void deleteByPermissionIds(@Param("permissionIds") List<Integer> permissionIds);

}
