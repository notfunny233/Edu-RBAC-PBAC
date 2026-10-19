package eduadmin.Controller;

import eduadmin.Service.EmpService;
import eduadmin.anno.RequiresPermission;
import eduadmin.pojo.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.annotations.Delete;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

@Slf4j
@RequestMapping("/emps")
@RestController
public class EmpController {
    @Autowired
    private EmpService empService;


    /*@GetMapping
    public Result page(String name, Integer gender,
                        @DateTimeFormat(pattern = "yyyy-MM-dd")LocalDate begin,
                        @DateTimeFormat(pattern = "yyyy-MM-dd")LocalDate end,
                        @RequestParam(defaultValue = "1") Integer page,
                        @RequestParam(defaultValue = "10") Integer pageSize) {
        log.info("条件查询,{},{},{},{},{},{}",name,gender,begin,end,page,pageSize);
        PageResult<Emp> tpageResult=empService.page(name,gender,begin,end,page,pageSize);
        return Result.success(tpageResult);*/
    @RequiresPermission("emp:list")
    @GetMapping
    //分页查询
    public Result pageEmp(EmpQueryParam empQueryParam) {
        log.info("条件查询,{}", empQueryParam);
        PageResult<Emp> pageResult = empService.page(empQueryParam);
        return Result.success(pageResult);
    }

    @RequiresPermission("emp:add")
    @PostMapping
    //插入员工信息
    public Result saveEmp(@RequestBody Emp emp) {
        log.info("员工基本信息,{}", emp);
        empService.save(emp);
        return Result.success();
    }

    @RequiresPermission("emp:delete")
    @DeleteMapping
    //删除员工
    //?ids=1&ids=2&ids=3`
    public Result deleteEmp(@RequestParam List<Integer> ids){
        log.info("请求的id数组{}",ids);
        empService.del(ids);
        return Result.success();
    }

    //查询回显
    @RequiresPermission("emp:list")
    @GetMapping("/{id}")
    public Result getInfo(@PathVariable Integer id){
        log.info("根据ID查询员工数据: {}",id);
        Emp emp = empService.getempById(id);
        return Result.success(emp);
    }
    //更新员工信息
    @RequiresPermission("emp:update")
    @PutMapping
    public Result updateEmp(@RequestBody Emp emp){
        log.info("更新员工信息: {}",emp);
        empService.update(emp);
        return Result.success();
    }

    //这个接口是给"新增班级时选班主任"用的下拉数据，能看员工列表就够了，不额外要求 emp:add
    @RequiresPermission("emp:list")
    @GetMapping("/list")
    public Result findall() {
        log.info("查询全部员工数据");
        List<Emp> list = empService.findall();
        return Result.success(list);
    }

}
