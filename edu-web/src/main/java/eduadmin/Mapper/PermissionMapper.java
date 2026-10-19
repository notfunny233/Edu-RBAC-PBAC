package eduadmin.Mapper;

import eduadmin.pojo.Permission;
import eduadmin.pojo.PermissionQueryParam;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface PermissionMapper {

    //xml注入(条件查询)
    List<Permission> list(PermissionQueryParam permissionQueryParam);

    //插入权限点基本信息
    @Options(useGeneratedKeys = true, keyProperty = "id")
    @Insert("INSERT INTO permission(name, code, type, parent_id, sort, remark, create_time, update_time)" +
            "value(#{name},#{code},#{type},#{parentId},#{sort},#{remark},#{createTime},#{updateTime})")
    void insert(Permission permission);

    //根据id查询回显权限点数据
    Permission getById(Integer id);

    //更新权限点基本信息
    void update(Permission permission);

    //（批量）删除权限点
    void del(List<Integer> ids);

    //查询全部权限点，用于"给角色授权限点"的勾选树
    @Select("select id, name, code, type, parent_id, sort, remark, create_time, update_time " +
            "from permission order by sort, id")
    List<Permission> findall();

    //查询指定权限点ID集合对应的编码
    //用于删除前的保护性校验：'*' 通配权限点被删掉，超级管理员会瞬间失去全部权限
    List<String> selectCodesByIds(List<Integer> ids);

    //校验权限点编码是否已被别的权限点占用（新增时 id 传 0，含义同 RoleMapper）
    @Select("select count(*) from permission where code = #{code} and id != #{id}")
    int countByCodeExcludeId(@Param("code") String code, @Param("id") Integer id);

}
