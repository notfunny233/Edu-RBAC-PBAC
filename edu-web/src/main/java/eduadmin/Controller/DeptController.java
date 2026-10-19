package eduadmin.Controller;

import eduadmin.Service.DeptService;
import eduadmin.anno.Log;
import eduadmin.anno.RequiresPermission;
import eduadmin.pojo.Dept;
import eduadmin.pojo.Result;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
@Slf4j
@RequestMapping("/depts")
@RestController//相当于加了两个注解
                  //Controller:交给IOC容器
                  //ResponseBody:将方法返回值直接响应给前端，如果返回值是一个集合或者对象则先转为JSON在响应
public class DeptController {

    //private static final Logger log = LoggerFactory.getLogger(DeptController.class);
    //日志技术用注解替代

    @Autowired
    private DeptService deptService;

    //@RequestMapping(value = "/depts",method = RequestMethod.GET)
    //权限点编码规律：资源:动作。资源取路由第一段(depts->dept)，动作取HTTP语义
    @RequiresPermission("dept:list")
    @GetMapping
    public Result Deptlist(){
       //System.out.println("查询全部的部门数据");
        log.info("查询全部的部门数据");
        List<Dept> deptList=deptService.findAll();
        return Result.success(deptList);
    }
    //删除
    @Log
    @RequiresPermission("dept:delete")
    @DeleteMapping
    public Result deleteDept(Integer id){
        //System.out.println("删除的部门ID为:"+id);
        log.info("删除的部门ID为:{}",id);//占位符简化+id
        deptService.delById(id);
        return Result.success();

    }
    //插入
    @Log
    @RequiresPermission("dept:add")
    @PostMapping
    public Result insertDept(@RequestBody Dept dept){
        //System.out.println("新增部门"+dept);
        log.info("新增部门:{}",dept);
        deptService.add(dept);
        return Result.success();
    }
    //查询回显
    @RequiresPermission("dept:list")
    @GetMapping("/{id}")
    public Result getInfo(@PathVariable Integer id){
        //System.out.println("根据ID查询部门数据: " + id);
        log.info("根据ID查询部门数据: {}",id);
        Dept dept = deptService.getById(id);
        return Result.success(dept);
    }
    //更新
    @Log
    @RequiresPermission("dept:update")
    @PutMapping
    public Result updateDept(@RequestBody Dept dept){
        //System.out.println("更新部门"+dept);
        log.info("更新部门: {}",dept);
        deptService.update(dept);
        return Result.success();
    }





}
