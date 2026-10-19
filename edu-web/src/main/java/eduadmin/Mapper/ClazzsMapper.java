package eduadmin.Mapper;


import eduadmin.pojo.Clazz;
import eduadmin.pojo.ClazzQueryParam;
import eduadmin.pojo.PageResult;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ClazzsMapper {

    List<Clazz> list(ClazzQueryParam clazzQueryParam);

    @Delete("delete from clazz where id=#{id}")
    void deleteClazz(String id);

    @Insert(
            "insert into clazz(name,room,begin_date,end_date,master_id,create_time,subject)" +
            "value( #{name},#{room},#{beginDate},#{endDate},#{masterId},now(),#{subject})")
    void insertClazz(Clazz clazz);

    @Select("SELECT c.* from clazz c where id=#{id}")
    Clazz selectClazzbyid(Integer id);

    void updateclazz(Clazz clazz);

    @Select("select c.* from clazz c")
    List<Clazz> selectall();
}
