package eduadmin.Controller;

import eduadmin.Service.RoleService;
import eduadmin.anno.Log;
import eduadmin.anno.RequiresPermission;
import eduadmin.pojo.PageResult;
import eduadmin.pojo.Result;
import eduadmin.pojo.Role;
import eduadmin.pojo.RoleQueryParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 角色管理
 * <p>
 * 每个接口上都能看到它需要哪个权限点，这也是把注解写在方法上的好处：
 * 不用翻拦截器配置去猜"这个接口到底谁能调"。
 * <p>
 * 权限点编码的规律是 {@code 资源:动作}，与路由对应：
 * /roles -> role，GET -> list，POST -> add，PUT -> update，DELETE -> delete
 */
@Slf4j
@RestController
@RequestMapping("/roles")
public class RoleController {

    @Autowired
    private RoleService roleService;

    @RequiresPermission("role:list")
    @GetMapping
    //分页查询
    public Result pageRole(RoleQueryParam roleQueryParam) {
        log.info("条件查询,{}", roleQueryParam);
        PageResult<Role> pageResult = roleService.page(roleQueryParam);
        return Result.success(pageResult);
    }

    @RequiresPermission("role:add")
    @Log
    @PostMapping
    //插入Role信息
    public Result saveRole(@RequestBody Role role) {
        log.info("Role基本信息,{}", role);
        roleService.save(role);
        return Result.success();
    }

    //查询回显
    @RequiresPermission("role:list")
    @GetMapping("/{id}")
    public Result getInfo(@PathVariable Integer id) {
        log.info("根据ID查询Role数据: {}", id);
        Role role = roleService.getById(id);
        return Result.success(role);
    }

    //更新Role信息
    @RequiresPermission("role:update")
    @Log
    @PutMapping
    public Result updateRole(@RequestBody Role role) {
        log.info("更新Role信息: {}", role);
        roleService.update(role);
        return Result.success();
    }

    //删除Role
    //?ids=1&ids=2&ids=3
    @RequiresPermission("role:delete")
    @Log
    @DeleteMapping
    public Result deleteRole(@RequestParam List<Integer> ids) {
        log.info("请求的id数组{}", ids);
        roleService.del(ids);
        return Result.success();
    }

    //查询全部角色，供"给员工分配角色"的下拉选择使用
    //路径是 /roles/list，能精确匹配上而不会被 /{id} 抢走（Spring 优先匹配字面量路径）
    @RequiresPermission("role:list")
    @GetMapping("/list")
    public Result findall() {
        log.info("查询全部角色");
        List<Role> list = roleService.findall();
        return Result.success(list);
    }

}
