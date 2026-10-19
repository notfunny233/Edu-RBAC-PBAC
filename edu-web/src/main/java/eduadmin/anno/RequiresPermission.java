package eduadmin.anno;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 权限校验注解。
 * <p>
 * 写在 Controller 方法上，PermissionInterceptor 会读到它，再交给 PolicyDecisionPoint 判断。
 * <p>
 * 为什么用注解，而不是在拦截器里判断请求路径？
 * 因为权限本来就是"接口自己的属性"，写在方法上离代码最近：
 * 新增接口时能直接看到它需要什么权限，删接口时注解跟着一起删掉；
 * 也不会出现"拦截器里配的是 /emps，但 Controller 早就改名成 /emps2 了"这种对不上的情况。
 * <p>
 * value 的格式是 resource:action，例如 @RequiresPermission("emp:delete")。
 * 引擎会把它拆成 resource=emp、action=delete，用来匹配权限点和策略。
 * <p>
 * 也可以写在类上，表示这个 Controller 下所有接口都要求该权限。
 * 方法上的注解优先于类上的注解。
 */
@Target({ElementType.METHOD, ElementType.TYPE})//方法上优先；类上表示整个 Controller 统一要求
@Retention(RetentionPolicy.RUNTIME)//运行时保留，拦截器才能通过反射读到它
public @interface RequiresPermission {
    String value(); //权限点编码，格式 resource:action
}
