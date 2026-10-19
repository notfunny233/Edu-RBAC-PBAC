package eduadmin.Service.Impl;

import eduadmin.Mapper.Deptmapper;
import eduadmin.Service.DeptService;
import eduadmin.pojo.Dept;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service//交给IOC容器管理
public class DeptServiceImpl implements DeptService {

    @Autowired
    private Deptmapper deptmapper;

    @Override
    public List<Dept> findAll() {
        return deptmapper.FindAll();
    }

    @Override
    public void delById(Integer id) {
        deptmapper.DEL(id);
    }

    @Override
    public void add(Dept dept) {
        //1. 补全基础属性 - createTime, updateTime
        dept.setCreateTime(LocalDateTime.now());
        dept.setUpdateTime(LocalDateTime.now());
        //2. 调用Mapper接口方法插入数据
        deptmapper.insert(dept);
    }
    @Override
    public Dept getById(Integer id) {
        return deptmapper.getById(id);
    }

    @Override
    public void update(Dept dept) {
        //1. 补全基础属性 -  updateTime
        dept.setUpdateTime(LocalDateTime.now());
        //2. 调用Mapper接口方法插入数据
        deptmapper.update(dept);
    }


}
