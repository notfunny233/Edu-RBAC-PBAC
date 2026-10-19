package eduadmin.Controller;

import eduadmin.Service.PermissionService;
import eduadmin.anno.Log;
import eduadmin.anno.RequiresPermission;
import eduadmin.pojo.PageResult;
import eduadmin.pojo.Permission;
import eduadmin.pojo.PermissionQueryParam;
import eduadmin.pojo.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 权限点管理
 * <p>
 * 权限点是这套体系的最小颗粒度，编码约定 {@code resource:action}。
 * 决策引擎判断"有没有权限"就是拿接口注解上的编码去和这里的编码比对，
 * 所以这张表里的编码一旦改动，所有依赖它的角色授权都会立刻失效（关联表按ID绑的，
 * 改code不会断关联，但接口上的注解还是老编码，两边就对不上了）——
 * 修改编码属于高风险操作，线上一般只新增不修改。
 */
@Slf4j
@RestController
@RequestMapping("/permissions")
public class PermissionController {

    @Autowired
    private PermissionService permissionService;

    @RequiresPermission("permission:list")
    @GetMapping
    //分页查询
    public Result pagePermission(PermissionQueryParam permissionQueryParam) {
        log.info("条件查询,{}", permissionQueryParam);
        PageResult<Permission> pageResult = permissionService.page(permissionQueryParam);
        return Result.success(pageResult);
    }

    @RequiresPermission("permission:add")
    @Log
    @PostMapping
    //插入Permission信息
    public Result savePermission(@RequestBody Permission permission) {
        log.info("Permission基本信息,{}", permission);
        permissionService.save(permission);
        return Result.success();
    }

    //查询回显
    @RequiresPermission("permission:list")
    @GetMapping("/{id}")
    public Result getInfo(@PathVariable Integer id) {
        log.info("根据ID查询Permission数据: {}", id);
        Permission permission = permissionService.getById(id);
        return Result.success(permission);
    }

    //更新Permission信息
    @RequiresPermission("permission:update")
    @Log
    @PutMapping
    public Result updatePermission(@RequestBody Permission permission) {
        log.info("更新Permission信息: {}", permission);
        permissionService.update(permission);
        return Result.success();
    }

    //删除Permission
    //?ids=1&ids=2&ids=3
    @RequiresPermission("permission:delete")
    @Log
    @DeleteMapping
    public Result deletePermission(@RequestParam List<Integer> ids) {
        log.info("请求的id数组{}", ids);
        permissionService.del(ids);
        return Result.success();
    }

    //查询全部权限点，供"给角色授权限点"的勾选列表使用
    @RequiresPermission("role:list")
    @GetMapping("/list")
    public Result findall() {
        log.info("查询全部权限点");
        List<Permission> list = permissionService.findall();
        return Result.success(list);
    }

}
