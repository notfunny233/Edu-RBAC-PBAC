package eduadmin.Controller;


import eduadmin.Service.ClazzsService;
import eduadmin.anno.RequiresPermission;
import eduadmin.pojo.Clazz;
import eduadmin.pojo.ClazzQueryParam;
import eduadmin.pojo.PageResult;
import eduadmin.pojo.Result;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.annotations.Delete;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/clazzs")
public class ClazzsController {
    @Autowired
    private ClazzsService clazzsService;

    @RequiresPermission("clazz:list")
    @GetMapping
    public Result findclass(ClazzQueryParam clazzQueryParam){//ClazzQueryParam分页条件查询，默认分页数据的实体类
        log.info("班级分页查询");
        PageResult<Clazz> pageResult=clazzsService.getclasslist(clazzQueryParam);
        return Result.success(pageResult);
    }

    @RequiresPermission("clazz:delete")
    @DeleteMapping("/{id}")
    public Result deleteClazz(@PathVariable String id) {
        log.info("删除班级");
        clazzsService.deleteClazz(id);
        return Result.success();
    }

    @RequiresPermission("clazz:add")
    @PostMapping
    public Result savaClazz(@RequestBody Clazz clazz){
        log.info("新增班级");
        clazzsService.saveClazz(clazz);
        return Result.success();
    }
    //查询回显
    @RequiresPermission("clazz:list")
    @GetMapping("/{id}")
    public Result getClazzbyID(@PathVariable Integer id){
        log.info("根据ID查询班级");
        Clazz clazz=clazzsService.getClazzbyID(id);
        return Result.success(clazz);
    }

    @RequiresPermission("clazz:update")
    @PutMapping
    public Result updateclazz(@RequestBody Clazz clazz){
        log.info("根据ID查询班级");
        clazzsService.updateClazz(clazz);
        return Result.success();
    }
    //此接口用于新建学生
    @RequiresPermission("clazz:list")
    @GetMapping("/list")
    public Result findall(){
        log.info("查询所有班级");
        List<Clazz> list =clazzsService.findall();
        return Result.success(list);


    }
}
