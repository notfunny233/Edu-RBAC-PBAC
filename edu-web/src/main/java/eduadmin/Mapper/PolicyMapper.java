package eduadmin.Mapper;

import eduadmin.pojo.Policy;
import eduadmin.pojo.PolicyQueryParam;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface PolicyMapper {

    //xml注入(条件查询)
    List<Policy> list(PolicyQueryParam policyQueryParam);

    //插入策略基本信息
    @Options(useGeneratedKeys = true, keyProperty = "id")
    @Insert("INSERT INTO policy(name, subject_type, subject_id, resource, action, effect, condition_expr, priority, status, remark, create_time, update_time)" +
            "value(#{name},#{subjectType},#{subjectId},#{resource},#{action},#{effect},#{conditionExpr},#{priority},#{status},#{remark},#{createTime},#{updateTime})")
    void insert(Policy policy);

    //根据id查询回显策略数据
    Policy getById(Integer id);

    //更新策略基本信息
    void update(Policy policy);

    //（批量）删除策略
    void del(List<Integer> ids);

    //校验策略名称是否已被别的策略占用（表上有 uk_policy_name 唯一键，这里提前拦一道给出友好提示）
    @Select("select count(*) from policy where name = #{name} and id != #{id}")
    int countByNameExcludeId(@Param("name") String name, @Param("id") Integer id);

    /**
     * 决策引擎专用：查出这次请求可能命中的策略，按优先级从高到低排序
     * <p>
     * 四个筛选条件：
     * 1. status = 1           只取启用的策略，停用的策略要能"留着但不生效"
     * 2. resource 匹配        支持策略里写 * 表示不限资源
     * 3. action 匹配          同样支持 *
     * 4. 主体匹配             策略主体是当前员工的某个角色，或者直接就是这个员工
     * <p>
     * 注意 @Param 不能省：这个方法有 4 个参数，MyBatis 不做参数名推断，
     * 不写 @Param 会在 XML 里报 "Parameter 'resource' not found"
     */
    List<Policy> selectMatched(@Param("resource") String resource,
                               @Param("action") String action,
                               @Param("empId") Integer empId,
                               @Param("roleIds") List<Integer> roleIds);

}
