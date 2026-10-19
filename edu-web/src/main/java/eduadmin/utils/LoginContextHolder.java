package eduadmin.utils;

import eduadmin.pojo.Emp;

/**
 * 登录人上下文：把"当前登录的是谁"存在当前线程上。
 * <p>
 * 为什么需要它：
 * TokenInterceptor 校验完令牌，只知道自己放行了，但后面很多地方都要用"当前登录人"，
 * 比如权限判定、操作日志要记是谁操作的。
 * 如果一层层往方法参数里传这个人，所有相关方法签名都得改，所以用 ThreadLocal 挂在线程上。
 * <p>
 * 为什么这样是安全的：
 * Tomcat 处理一个请求时，从拦截器到 Controller 再到 Service 都在同一条线程上，
 * 所以请求进来时存、处理过程中取，拿到的肯定是同一个人的数据。
 * <p>
 * 但有一个必须注意的问题：
 * Tomcat 用线程池，请求处理完线程会被复用去处理下一个请求。
 * 如果只存不删，下一个请求可能读到上一个人的登录信息，
 * 就会出现"偶尔能看到别人数据"的越权问题，而且这种情况很难复现。
 * 所以 TokenInterceptor 里存和删必须成对出现，下面 clear() 就是干这个的，不要删。
 */
public class LoginContextHolder {

    /** 当前线程的登录人。存整个 Emp 而不只存 id，是因为策略条件要读部门、职位等属性，可以少查一次库 */
    //ThreadLocal 是什么：按线程分格的储物柜，每条线程都有自己的储物柜，互不干扰。
    private static final ThreadLocal<Emp> CURRENT_EMP = new ThreadLocal<>();//类似集合的容器，每条线程都有但又互相独立。

    /**
     * 工具类不允许被实例化：方法全是静态的，new 出来没有意义
     */
    private LoginContextHolder() {
    }

    /**
     * 存入当前登录人（由 TokenInterceptor 在校验通过后调用）
     */
    public static void setEmp(Emp emp) {
        CURRENT_EMP.set(emp);
    }//存入当前线程的登录人

    /**
     * 取出当前登录人，未登录时返回 null（调用方要判空）
     */
    public static Emp getEmp() {
        return CURRENT_EMP.get();
    }//取出当前线程的登录人

    /**
     * 取出当前登录人的 id，未登录时返回 null
     * 操作日志切面用它替换掉原来写死的 1
     */
    public static Integer getEmpId() {
        Emp emp = CURRENT_EMP.get();
        return emp == null ? null : emp.getId();//获取当前登录人的id，先用三元运算符判断是否为空
    }

    /**
     * 清掉当前线程的登录人。必须在请求结束时调用，否则线程复用时会把数据带给下一个请求。
     */
    public static void clear() {
        CURRENT_EMP.remove();
    }//清掉当前线程的登录人
}
