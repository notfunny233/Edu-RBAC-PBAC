package eduadmin.Aop;

import eduadmin.Mapper.OperateLogMapper;
import eduadmin.pojo.OperateLog;
import eduadmin.utils.LoginContextHolder;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Arrays;

@Slf4j
@Aspect
@Component
public class OperateLogAspect {

    @Autowired
    private OperateLogMapper operateLogMapper;


    @Pointcut("@annotation(eduadmin.anno.Log)")
    public void logPointCut(){}

    @Around("logPointCut()")
    public Object recordLog(ProceedingJoinPoint joinPoint) throws Throwable {
        // 1. 记录开始时间
        long start = System.currentTimeMillis();

        // 2. 执行目标方法
        Object result;
        try {
            result = joinPoint.proceed();
        } catch (Throwable e) {
            // 方法抛出异常依然继续记录日志（你也可以选择异常不保存日志）
            throw e;
        }

        // 3. 计算耗时
        long end = System.currentTimeMillis();
        long costTime = end - start;

        // 4. 封装OperateLog实体
        OperateLog operateLog = new OperateLog();
        // 操作人ID：从登录上下文获取，示例为ThreadLocal，替换成你项目的登录工具类
        operateLog.setOperateEmpId(getCurrentEmpId());
        operateLog.setOperateTime(LocalDateTime.now());

        // 获取目标类全类名
        operateLog.setClassName(joinPoint.getTarget().getClass().getName());
        // 获取方法名
        operateLog.setMethodName(joinPoint.getSignature().getName());
        // 方法参数，转JSON字符串，截断2000字符
        operateLog.setMethodParams(Arrays.toString(joinPoint.getArgs()));
        // 返回值转JSON，截断2000字符
        operateLog.setReturnValue(result != null ? result.toString() : "void");

        operateLog.setCostTime(costTime);

        // 插入数据库
        log.info("记录的操作日志{}",operateLog);
        operateLogMapper.insert(operateLog);

        return result;
    }

    /**
     * 获取当前登录用户ID
     * <p>
     * 原来这里写死 return 1，导致库里所有操作日志的操作人都是同一个人。
     * 审计日志最关键的字段就是"谁干的"，这个字段是假的，整条功能就没有意义了。
     * 现在登录人由 TokenInterceptor 在令牌校验通过后放进 LoginContextHolder。
     */
    private Integer getCurrentEmpId() {
        // return UserContext.getEmpId();
        Integer empId = LoginContextHolder.getEmpId();
        // 取不到时返回 null，而不是随便编一个ID：
        // operate_emp_id 允许为空，空值能明确表达"这次操作没识别到操作人"；
        // 塞一个假ID反而更危险，排查时会让人把责任追到一个无辜的员工身上
        return empId;
    }
}
