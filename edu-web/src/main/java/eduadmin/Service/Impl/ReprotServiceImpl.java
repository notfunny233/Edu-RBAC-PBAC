package eduadmin.Service.Impl;

import eduadmin.Mapper.EmpMapper;
import eduadmin.Mapper.StudentMapper;
import eduadmin.Service.ReprotService;
import eduadmin.pojo.EmpGender;
import eduadmin.pojo.JobOption;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class ReprotServiceImpl implements ReprotService {
    @Autowired
    private EmpMapper empMapper;

    @Override
    public JobOption empJobData() {
        //获取统计数据object是因为java拿到的值为varchar及String
        List<Map<String, Object>> jobDataList = empMapper.getJobDataList();
        //组装结果
        List<Object> jobList = jobDataList.stream().map(dataMap ->dataMap.get("pos")).toList();
        List<Object> dataList = jobDataList.stream().map(dataMap->dataMap.get("num")).toList();

        return new JobOption(jobList,dataList);
    }

    @Override
    public List<EmpGender> empGenderData() {
        //获得性别和数量的键值对集合
        List<EmpGender> List=empMapper.getGenderData();
        return List;
    }
}
