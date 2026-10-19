package eduadmin.Service;

import eduadmin.pojo.Dept;

import java.util.List;

public interface DeptService {
    List<Dept> findAll();
    void delById(Integer id);
    void add(Dept dept);
    Dept getById(Integer id);
    void update(Dept dept);
}
