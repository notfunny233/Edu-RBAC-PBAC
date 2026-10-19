package eduadmin.Service;

import eduadmin.pojo.EmpGender;
import eduadmin.pojo.JobOption;

import java.util.List;
import java.util.Map;

public interface ReprotService {
    //获取职位名称和员工信息键值对
    JobOption empJobData();
    //获取性别和数量键值对
    List<EmpGender> empGenderData();
}
