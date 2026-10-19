package eduadmin.Service;


import eduadmin.pojo.*;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.util.List;

public interface EmpService {

    PageResult<Emp> page(EmpQueryParam empQueryParam);//分页条件查询
    void save(Emp emp);
    void del(List<Integer> ids);
    Emp getempById(Integer id);
    void update(Emp emp);
    Logininfo login(Emp emp);
    List<Emp> findall();
}
