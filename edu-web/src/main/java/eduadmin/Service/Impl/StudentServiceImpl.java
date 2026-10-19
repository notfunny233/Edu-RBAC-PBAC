package eduadmin.Service.Impl;


import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import eduadmin.Mapper.StudentMapper;
import eduadmin.Service.StudentService;
import eduadmin.pojo.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class StudentServiceImpl implements StudentService {

    @Autowired
    private StudentMapper studentMapper;


    @Override
    public PageResult<Students> findStudent(StudentQueryParam studentQueryParam) {
        //分页配置
        PageHelper.startPage(studentQueryParam.getPage(), studentQueryParam.getPageSize());
        //调用Service
        List<Students> list = studentMapper.selectStudent(studentQueryParam);
        //转换类型返回
        PageInfo<Students> pageInfo = new PageInfo<>(list);

        return new PageResult<Students>(pageInfo.getTotal(), pageInfo.getList());
    }

    @Override
    public void deleteStudents(List<Integer> ids) {
        studentMapper.delStus(ids);
    }

    @Override
    public void insertStudent(Students students) {
        studentMapper.insertStu(students);

    }

    @Override
    public Students getstubyId(Integer id) {
        Students stu = studentMapper.getstubyId(id);
        return stu;
    }

    @Override
    public void updatestu(Students stu) {
        studentMapper.updatestu(stu);
    }

    @Override
    public void deductScore(Integer id, Integer score) {
        studentMapper.updatestuscore(id, score);
    }

    @Override
    public List<Map<String, Integer>> studentDegreeData() {
        //获取学员姓名学历
        List<Map<String,Integer>> list= studentMapper.getDegreeData();
        return list;
    }

    @Override
    public CountData studentCountData() {
        //获取学员姓名学历
        List<Map<String, Long>> list= studentMapper.getCountData();
        ArrayList namelist = new ArrayList<String>();
        ArrayList valuelist = new ArrayList<Integer>();
        list.forEach(map->{
                namelist.add(map.get("name"));
                valuelist.add(map.get("value"));
        });

        return new CountData(namelist,valuelist );

    }
}
