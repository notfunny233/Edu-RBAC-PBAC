package eduadmin.utils;

import eduadmin.Mapper.PolicyMapper;
import eduadmin.pojo.Emp;
import eduadmin.pojo.Policy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.PropertyAccessor;
import org.springframework.expression.TypedValue;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 权限判定的核心类。外部只调 decide() 这一个方法。
 * <p>
 * 判定的四步，全部通过才放行：
 * 1. 取当前登录人。取不到就拒绝
 * 2. 查这个人有没有本次要的权限点。没有就拒绝（这是 RBAC 部分）
 * 3. 查有没有策略要限制这次操作。被策略拒绝就拒绝（这是 PBAC 部分）
 * 4. 以上都通过就放行
 * <p>
 * RBAC 和 PBAC 是"叠加"，不是"二选一"：
 * RBAC 回答"有没有这个功能"，PBAC 回答"这个功能上有没有条件限制"。
 * <p>
 * 为什么放在 utils 包：项目分层只有 Controller / Service / Mapper，
 * 新加一层会破坏既有约定；utils 包里本来就有 @Component 的类（AliyunOSSOperator）。
 */
@Slf4j
@Component
public class PolicyDecisionPoint {

    /*通配权限点。拥有它就不用再逐项授权了 */
    private static final String ALL = "*";

    /*权限点里资源和操作的分隔符，约定是 resource:action */
    private static final String SEPARATOR = ":";

    @Autowired
    private PolicyMapper policyMapper;

    /*权限数据的缓存。角色和权限点只在授权时变，但每个请求都要读，走缓存省掉两次查询*/
    @Autowired
    private PermissionCache permissionCache;

    /*解析表达式的工具。它是线程安全的，所以定义成字段复用。
      不要每次判定都 new 一个：这里每个请求都会走到，属于热点代码。*/

    private final ExpressionParser expressionParser = new SpelExpressionParser();


    //参数介绍:PermissionInterceptor拦截器获取的注释参数(权限点)和整理成Map的请求参数params
    public boolean decide(String permissionCode, Map<String, Object> params) {
        //第 1 步：取当前登录人
        Emp emp = LoginContextHolder.getEmp();//ThreadLocal容器方法类获取本线程的用户
        if (emp == null) {
            log.info("权限判定拒绝: 当前线程没有登录人上下文, 权限点:{}", permissionCode);
            return false;
        }

        //把权限点拆成资源和操作，后面匹配权限点和匹配策略都要用
        String resource = permissionCode;
        String action = "";
        int index = permissionCode == null ? -1 : permissionCode.indexOf(SEPARATOR);//找到分隔符，开头定义的常量
        if (index > 0) {
            resource = permissionCode.substring(0, index);//资源名
            action = permissionCode.substring(index + 1);//操作方法名
        }

        //第 2 步：查这个人有哪些权限点（先根据员工查有几个角色，再根据角色查有多少权限点）

        /*根据员工ID查询角色ID集合（roleId）select role_id from emp_role where emp_id = #{empId}
        一个员工可能有多个角色，roleIds不能为空不然sql报错
        这里走缓存：命中就直接返回，没命中才查库，数据只在授权接口里变*/
        List<Integer> roleIds = permissionCache.loadRoleIds(emp.getId());//用户有什么角色

        if (roleIds == null || roleIds.isEmpty()) {
            log.info("权限判定拒绝: 员工[{}]未分配任何角色, 权限点:{}", emp.getId(), permissionCode);
            return false;
        }
        //根据角色ID集合查询权限点集合，同样走缓存
        List<String> codes = permissionCache.loadCodes(roleIds);//用户有什么权限点
        if (!matchPermission(codes, resource, action)) {
            log.info("权限判定拒绝: 员工[{}]缺少权限点{}, 权限点:{}", emp.getId(), permissionCode, permissionCode);
            return false;
        }

        //第 3 步：查有没有策略限制。
        //只查启用的、资源和操作对得上的、主体是本人或本人角色的策略，按 priority 从大到小排存入policies集合
        List<Policy> policies = policyMapper.selectMatched(resource, action, emp.getId(), roleIds);
        boolean allowed = evaluatePolicies(policies, emp, params);
        if (!allowed) {
            log.info("权限判定拒绝: 员工[{}]被策略拦截, 权限点:{}", emp.getId(), permissionCode);
        }
        return allowed;
    }

    /**
     * 判断权限点是否命中，支持三种写法：
     * "*"           全部权限（超级管理员）
     * "emp:*"       emp 下面的所有操作
     * "emp:delete"  精确匹配
     */
    private boolean matchPermission(List<String> codes, String resource, String action) {
        if (codes == null || codes.isEmpty()) {//用户没有任何权限，直接拦截
            return false;
        }
        String resourceAll = resource + SEPARATOR + ALL;//资源所有权限
        String exact = resource + SEPARATOR + action;//拼接传进来资源的权限点
        for (String code : codes) {
            //满足最高权限，本资源最高权限，本资源本方法权限任意一个权限点就通过
            if (ALL.equals(code) || resourceAll.equals(code) || exact.equals(code)) {
                return true;//权限匹配成功
            }
        }
        return false;//否则拦截
    }

    /**
     * 根据策略判断是否放行。
     * <p>
     * 前提：权限点在第 2 步已经查过了，这里只是额外的条件限制。
     * <p>
     * 规则有两条：
     * 1. 按 priority 从大到小依次看，遇到第一条"条件成立"的策略就采用它，看它是允许还是拒绝。
     *    这样"高优先级的允许"可以覆盖"低优先级的拒绝"，规则冲突时有明确顺序。
     * 2. 一条都没命中，或者命中了但条件都不成立，就放行。
     *    因为策略只是"额外限制"，不是"白名单"。如果改成"没配策略就拒绝"，
     *    那每新增一个接口都得先配一条策略才能用，维护起来很累。
     * <p>
     * 注意：超级管理员不跳过策略，持有 * 权限点的人一样受策略限制。
     * 这样规则里没有特例，出问题时容易排查。
     * 代价是不能给超管所属角色配拒绝策略，否则会把自己锁在门外
     * （真锁了也不怕，把那条策略的 status 改成 0 就恢复了）。
     */
    private boolean evaluatePolicies(List<Policy> policies, Emp emp, Map<String, Object> params) {
        if (policies == null || policies.isEmpty()) {
            return true;//策略集合为空直接放行
        }
        for (Policy policy : policies) {
            Boolean conditionResult = evaluateCondition(policy.getConditionExpr(), emp, params);
            //求值出错时直接按拒绝处理，不要跳过（否则等于留了个后门）
            if (conditionResult == null) {
                log.error("策略[{}]条件表达式求值异常, 按拒绝处理, expr:{}", policy.getName(), policy.getConditionExpr());
                return false;
            }
            if (!conditionResult) {
                continue; //条件不成立，这条策略这次不生效，继续看下一条
            }
            boolean allow = policy.getEffect() == null || policy.getEffect() == 1;
            log.info("权限判定命中策略[{}], 结果:{}", policy.getName(), allow ? "允许" : "拒绝");
            return allow;
        }
        //有策略但条件都不成立，说明不构成限制
        return true;
    }

    /**
     * 计算一条策略的条件表达式
     *
     * @return true/false 是计算结果；返回 null 表示表达式出错，调用方按拒绝处理
     */
    private Boolean evaluateCondition(String conditionExpr, Emp emp, Map<String, Object> params) {//conditionExpr字段表示在什么条件下生效
        //条件为空表示无条件生效。例如"班主任不得删除部门"这种一刀切的规则
        if (conditionExpr == null || conditionExpr.trim().isEmpty()) {
            return Boolean.TRUE;
        }
        try {
            StandardEvaluationContext context = new StandardEvaluationContext();// 造出一个空容器，待会儿往里放变量。例如 #emp 和 #params
            context.addPropertyAccessor(new NullSafeMapPropertyAccessor());//注册自定义属性访问器，替换 SpEL 默认对 Map 的取值行为。
            //把登录人对象和请求参数 Map 绑定为表达式里的 #emp、#params。
            context.setVariable("emp", emp);
            context.setVariable("params", params);
            Boolean result = expressionParser.parseExpression(conditionExpr).getValue(context, Boolean.class);//解析表达式字符串并求值，结果强转为 Boolean，作为方法的返回值。
            return result != null && result;
        } catch (Exception e) {
            //能走到这里说明表达式真有问题，比如字段名写错、类型不匹配
            log.error("策略条件表达式解析或求值失败, expr:{}", conditionExpr, e);
            return null;
        }
    }

    /**
     * 一个"取 Map 里不存在的键会返回 null，而不是报错"的访问器。
     * <p>
     * 它的作用只有一个：让 #params.deptId 这种写法在参数没传时得到 null，
     * 这样 #params.deptId != null 能正常算出 false，而不是抛异常。
     * <p>
     * 有一点副作用，但可以接受：因为对 Map 的任何属性名都返回"能读"，
     * 所以 #params.size 这类 Map 自带属性会取到 null。
     * 策略条件里只需要读请求参数，用不到 Map 自己的属性，所以不做额外区分。
     */
    private static class NullSafeMapPropertyAccessor implements PropertyAccessor {

        /** 只接管 Map 类型，其它对象（比如 #emp 这个 Emp 对象）还是走 SpEL 默认的取值方式 */
        @Override
        public Class<?>[] getSpecificTargetClasses() {
            return new Class<?>[]{Map.class};
        }

        @Override
        public boolean canRead(EvaluationContext context, Object target, String name) {
            return target instanceof Map;
        }

        @Override
        public TypedValue read(EvaluationContext context, Object target, String name) {
            //取不到的键就返回 null，不报错
            return new TypedValue(((Map<?, ?>) target).get(name));
        }

        @Override
        public boolean canWrite(EvaluationContext context, Object target, String name) {
            return false;
        }

        @Override
        public void write(EvaluationContext context, Object target, String name, Object newValue) {
            //策略条件只用来读，不允许表达式反过来改请求参数
            throw new UnsupportedOperationException("策略条件表达式为只读，不允许写入");
        }
    }
}
