package eduadmin.Controller;



import eduadmin.Service.ReprotService;
import eduadmin.Service.StudentService;
import eduadmin.anno.RequiresPermission;
import eduadmin.pojo.CountData;
import eduadmin.pojo.EmpGender;
import eduadmin.pojo.JobOption;
import eduadmin.pojo.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@Slf4j
@RequestMapping("report")
@RestController
public class ReportController {

    @Autowired
    private ReprotService reportService;
    @Autowired
    private StudentService studentService;

    //统计员工职位人数
    //报表类接口统一用 report:list 一个权限点。
    //它们不修改任何数据，风险在于"泄露组织信息"（职位分布、薪资结构等），
    //按模块拆成 4 个权限点意义不大，反而让授权的人多点几次
    @RequiresPermission("report:list")
    @GetMapping("/empJobData")
    public Result empJobData(){
        log.info("返回员工信息统计数据");
        JobOption jobOption =reportService.empJobData();
        return Result.success(jobOption);
    }

    //统计员工性别人数
    @RequiresPermission("report:list")
    @GetMapping("/empGenderData")
    public Result empGenderData(){
        log.info("返回员工性别统计数据");
        List<EmpGender> genderOption =reportService.empGenderData();
        return Result.success(genderOption);
    }

    @RequiresPermission("report:list")
    @GetMapping("/studentDegreeData")
    public Result studentDegreeData(){
        log.info("返回学员学历姓名");
        List<Map<String,Integer>> DegreeData = studentService.studentDegreeData();
        return Result.success(DegreeData);

    }

    @RequiresPermission("report:list")
    @GetMapping("/studentCountData")
    public Result studentCountData(){
        log.info("返回班级学员数量");
        CountData countData = studentService.studentCountData();
        return Result.success(countData);

    }
}
