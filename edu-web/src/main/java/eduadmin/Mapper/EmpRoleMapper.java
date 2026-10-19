package eduadmin.Mapper;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 员工-角色关联 Mapper
 * 关联表只有两个业务字段，没有独立实体，所以直接用 @Param 传参，不建 EmpRole 实体类
 */
@Mapper
public interface EmpRoleMapper {

    //根据员工ID查询其拥有的角色ID集合（决策引擎第一步要用它把"员工"换成"角色"）
    @Select("select role_id from emp_role where emp_id = #{empId}")
    List<Integer> selectRoleIdsByEmpId(Integer empId);

    //根据角色ID查询拥有该角色的员工ID集合（角色详情页展示"已分配员工"用）
    @Select("select emp_id from emp_role where role_id = #{roleId}")
    List<Integer> selectEmpIdsByRoleId(Integer roleId);

    //给员工批量授予角色（xml里用 foreach 拼批量插入，比循环调用少 N-1 次数据库往返）
    void insertBatch(@Param("empId") Integer empId, @Param("roleIds") List<Integer> roleIds);

    //清空某员工的全部角色关联
    //授权采用"覆盖式"：先清空再插入，逻辑简单且天然幂等，
    //比"算差集再增删"少一堆分支，代价是每次授权都要重写这几行，量级可以忽略
    @Delete("delete from emp_role where emp_id = #{empId}")
    void deleteByEmpId(Integer empId);

    //按员工ID批量删除关联，员工被删除时调用，避免留下指向已删员工的脏关联
    void deleteByEmpIds(@Param("empIds") List<Integer> empIds);

    //按角色ID批量删除关联，角色被删除时调用
    //做成批量而不是单个：删除角色接口支持一次删多个，循环调用会多出 N-1 次数据库往返
    void deleteByRoleIds(@Param("roleIds") List<Integer> roleIds);

}
