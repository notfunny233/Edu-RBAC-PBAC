package eduadmin.Mapper;

import eduadmin.pojo.Emp;
import eduadmin.pojo.EmpGender;
import eduadmin.pojo.EmpQueryParam;
import eduadmin.pojo.Logininfo;
import org.apache.ibatis.annotations.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Mapper
public interface EmpMapper {
//-------------------------------------------------------原始方法-----------------------------------------------------------------
    /**
     * 查询总记录数
     *//*
    @Select("select count(*) from emp e left join dept d on e.dept_id = d.id")
    public Long count();

    */

    /**
     * 分页查询
     *//*
    @Select("select e.*, d.name deptName from emp e left join dept d on e.dept_id = d.id order by e.update_time desc limit #{start},#{pageSize}")
    public List<Emp> list(Integer start, Integer pageSize);*/
//-------------------------------------------------------原始方法-----------------------------------------------------------------

    //xml注入(条件查询)
    public List<Emp> list(EmpQueryParam empQueryParam);

    //插入员工基本信息
    //`@Insert`本身**没有属性可以配置主键回填**，必须搭配 **`@Options`** 注解，等价于 XML 里的 `useGeneratedKeys="true" keyProperty="id"`。
    @Options(useGeneratedKeys = true, keyProperty = "id")
    @Insert("INSERT INTO emp(username, name, gender, phone, job, salary, image, entry_date, dept_id, create_time, update_time)" +
            "value(#{username},#{name},#{gender},#{phone},#{job},#{salary},#{image},#{entryDate},#{deptId},#{createTime},#{updateTime})")
    void empinfoinsert(Emp emp);

    //（批量）删除员工
    void del(List<Integer> ids);

    //根据id查询回显emp员工数据
    Emp getempById(Integer id);

    //更新员工基本信息
    void empupdate(Emp emp);

    List<Map<String,Object>> getJobDataList();

    List<EmpGender> getGenderData();

    @Select("select * from emp where username = #{username} and password = #{password}")
    Emp selectusernameandpassword(Emp emp);

}
