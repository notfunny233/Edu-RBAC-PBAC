package eduadmin.Mapper;

import eduadmin.pojo.StudentQueryParam;
import eduadmin.pojo.Students;
import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Map;

@Mapper
public interface StudentMapper {
    List<Map<String, Integer>> getDegreeData();

    public List<Students> selectStudent(StudentQueryParam studentQueryParam);

    void delStus(List<Integer> ids);

    @Insert("insert into student(name,no, gender, phone, degree, id_card, is_college, address, graduation_date, violation_count, violation_score, clazz_id, create_time, update_time) " +
            "value( #{name}, #{no}, #{gender}, #{phone}, #{degree}, #{idCard}, #{isCollege}, #{address}, #{graduationDate}, #{violationCount}, #{violationScore}, #{clazzId}, now(), now())")
    void insertStu(Students students);

    @Select("select s.* from student s where id=#{id}")
    Students getstubyId(Integer id);

    void updatestu(Students stu);

    //注意：拼接SQL时每段都要带空格。原来"set"结尾没空格，拼出来是 setviolation_score，
    //MySQL 当成一个列名，直接报 1064 语法错误，扣分接口一直用不了
    @Update("update student set " +
            "violation_score = IF(violation_score >= #{score}, violation_score - #{score}, 0), " +
            "violation_count = violation_count + 1 " +
            "where id = #{id}")
    void updatestuscore(@Param("id") Integer id, @Param("score") Integer score);

    List<Map<String, Long>> getCountData();
}
