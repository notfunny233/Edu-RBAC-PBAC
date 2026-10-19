package eduadmin.Controller;

import eduadmin.Service.PolicyService;
import eduadmin.anno.Log;
import eduadmin.anno.RequiresPermission;
import eduadmin.pojo.PageResult;
import eduadmin.pojo.Policy;
import eduadmin.pojo.PolicyQueryParam;
import eduadmin.pojo.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 权限策略管理
 * <p>
 * 这是 PBAC 相比 RBAC 多出来的那一层，也是最需要小心操作的一层：
 * 策略能覆盖权限点的判断结果（命中条件就把结果改成允许或拒绝），
 * 所以 policy:delete 这条权限等于"能关掉别人的约束"，授权时要谨慎。
 * <p>
 * policy:list 权限有它自己存在的意义：策略里可能包含部门、岗位这类组织信息，
 * 让所有人看到等于泄露内部管理规则，所以列表接口也要鉴权。
 */
@Slf4j
@RestController
@RequestMapping("/policies")
public class PolicyController {

    @Autowired
    private PolicyService policyService;

    @RequiresPermission("policy:list")
    @GetMapping
    //分页查询
    public Result pagePolicy(PolicyQueryParam policyQueryParam) {
        log.info("条件查询,{}", policyQueryParam);
        PageResult<Policy> pageResult = policyService.page(policyQueryParam);
        return Result.success(pageResult);
    }

    @RequiresPermission("policy:add")
    @Log
    @PostMapping
    //插入Policy信息
    public Result savePolicy(@RequestBody Policy policy) {
        log.info("Policy基本信息,{}", policy);
        policyService.save(policy);
        return Result.success();
    }

    //查询回显
    @RequiresPermission("policy:list")
    @GetMapping("/{id}")
    public Result getInfo(@PathVariable Integer id) {
        log.info("根据ID查询Policy数据: {}", id);
        Policy policy = policyService.getById(id);
        return Result.success(policy);
    }

    //更新Policy信息
    @RequiresPermission("policy:update")
    @Log
    @PutMapping
    public Result updatePolicy(@RequestBody Policy policy) {
        log.info("更新Policy信息: {}", policy);
        policyService.update(policy);
        return Result.success();
    }

    //删除Policy
    //?ids=1&ids=2&ids=3
    @RequiresPermission("policy:delete")
    @Log
    @DeleteMapping
    public Result deletePolicy(@RequestParam List<Integer> ids) {
        log.info("请求的id数组{}", ids);
        policyService.del(ids);
        return Result.success();
    }

}
