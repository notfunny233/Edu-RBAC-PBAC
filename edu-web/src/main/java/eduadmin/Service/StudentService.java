package eduadmin.Service;

import eduadmin.pojo.CountData;
import eduadmin.pojo.PageResult;
import eduadmin.pojo.StudentQueryParam;
import eduadmin.pojo.Students;

import java.util.List;
import java.util.Map;

public interface StudentService {
    PageResult<Students> findStudent(StudentQueryParam studentQueryParam);

    void deleteStudents(List<Integer> ids);
    void insertStudent(Students students);
    Students getstubyId(Integer id);
    void updatestu(Students stu);
    void deductScore(Integer id, Integer score);
    //获取学院姓名学历
    List<Map<String, Integer>> studentDegreeData();
    //获得班级学员数量
    CountData studentCountData();
}
