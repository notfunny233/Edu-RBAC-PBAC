package eduadmin.Mapper;

import eduadmin.pojo.Role;
import eduadmin.pojo.RoleQueryParam;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface RoleMapper {

    //xml注入(条件查询)
    List<Role> list(RoleQueryParam roleQueryParam);

    //插入角色基本信息
    //`@Insert`本身没有属性可以配置主键回填，必须搭配`@Options`注解，等价于 XML 里的 useGeneratedKeys="true" keyProperty="id"
    //授权时要拿到自增主键(roleId)才能写 role_permission，所以这里必须回填
    @Options(useGeneratedKeys = true, keyProperty = "id")
    @Insert("INSERT INTO role(name, code, remark, create_time, update_time)" +
            "value(#{name},#{code},#{remark},#{createTime},#{updateTime})")
    void insert(Role role);

    //根据id查询回显角色数据
    Role getById(Integer id);

    //更新角色基本信息
    void update(Role role);

    //（批量）删除角色
    void del(List<Integer> ids);

    //查询全部角色，用于"给员工分配角色"的下拉列表与回显
    @Select("select id, name, code, remark, create_time, update_time from role order by id")
    List<Role> findall();

    //查询指定角色ID集合对应的角色编码
    //用于删除前的保护性校验：超级管理员角色一旦被删，emp 1 就失去全部权限、
    //整个后台没人能再进授权页面，属于"删一下就要手工改数据库才能恢复"的事故
    List<String> selectCodesByIds(List<Integer> ids);

    //校验角色编码是否已被别的角色占用
    //新增时 id 传 0（自增主键从 1 开始，0 不可能命中任何一行，等价于"只查编码是否存在"）
    //修改时传自己的 id，把自己排除掉，否则改备注都会提示编码重复
    @Select("select count(*) from role where code = #{code} and id != #{id}")
    int countByCodeExcludeId(@Param("code") String code, @Param("id") Integer id);

}
