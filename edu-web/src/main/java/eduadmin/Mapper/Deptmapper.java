package eduadmin.Mapper;

import eduadmin.pojo.Dept;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface Deptmapper {

    @Select("Select id, name, create_time, update_time from dept where id=#{id}")
    Dept getById(Integer id);

    @Select("select id, name, create_time, update_time from dept order by update_time desc")
    List<Dept> FindAll();

    @Delete("delete from dept where id=#{id}")
    void DEL(Integer id);

    @Insert("INSERT INTO dept(name, create_time, update_time) values(#{name},#{createTime},#{updateTime})")
    void insert(Dept dept);
    //前端传回来的是id和新名字    更新时间为业务层逻辑
    @Update("UPDATE dept set name=#{name},update_time=#{updateTime} where id =#{id}")
    void update(Dept dept);

}

