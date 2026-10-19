package eduadmin.Interceptor;

import eduadmin.anno.RequiresPermission;
import eduadmin.utils.PolicyDecisionPoint;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;

import java.util.HashMap;
import java.util.Map;



//权限拦截器：读出接口上要求的权限点，交给决策引擎判断能否放行。
@Slf4j
@Component
public class PermissionInterceptor implements HandlerInterceptor {

    @Autowired
    private PolicyDecisionPoint policyDecisionPoint;


    /*权限总开关，默认开启。在 application.yaml 里配 pbac.enabled=false 可以关掉。
    留着它主要是为了排错：万一权限数据配乱了导致所有人都被拒绝，
    可以先关掉开关让系统能用，再去改数据，不用急着改代码重新打包。*/

    @Value("${pbac.enabled:true}")
    private boolean pbacEnabled;

    @Override           // 过滤器只有 HttpServletRequest，不知道请求最终交给哪个方法，拿不到 @RequiresPermission；
                        // 而 preHandle 的第三个参数 handler 就是目标方法
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        //开关关掉时直接放行，方便调试
        if (!pbacEnabled) {
            return true;
        }
        //如果handler不是HandlerMethod（不是Controller接口方法而是静态资源请求），直接 return true，放行。
        //如果是就强转HandlerMethod如果不是就返回 false
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }
        // 拿到这个方法上标记的 @RequiresPermission 自定义注解对象放入anno
        RequiresPermission anno = handlerMethod.getMethodAnnotation(RequiresPermission.class);

        // 方法上没有，退回类上的。方法优先于类是刻意的：
        if (anno == null) {
            anno = handlerMethod.getBeanType().getAnnotation(RequiresPermission.class);
        }

        // 没有注解表示这个接口不参与权限校验，直接放行。
        if (anno == null) {
            return true;
        }

        // 整理这次请求的参数，策略里的条件表达式要用
        Map<String, Object> params = buildParams(request);

        // 取出注解上标的权限点，连同参数交给决策引擎
        String permissionCode = anno.value();
        boolean allow = policyDecisionPoint.decide(permissionCode, params);
        // 决策引擎是从 LoginContextHolder 里取登录人的，而登录人是 TokenInterceptor 放进去的。
        // 所以注册时 TokenInterceptor 必须排在本拦截器前面，
        // 顺序反了这里每次都判成"没有登录人"，表现是所有接口都返回 403

        if (!allow) {//返回False(不满足条件表达式)
            log.info("权限校验不通过, 权限点:{}, 请求方式:{}, 请求路径:{}",
                    permissionCode, request.getMethod(), request.getRequestURI());
            // 返回 403 而不是 401：
            // 401 是"没登录"，前端该跳登录页；403 是"登录了但没权限"，前端弹个提示就行
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            // 响应体得自己写，因为这里 return false 不抛异常，走不到统一的异常处理
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":0,\"msg\":\"无操作权限，请联系管理员授权\"}");
            return false;
        }
        //返回Ture(满足条件表达式)放行
        //校验通过，放行
        return true;
    }


    /*把请求参数整理成 Map，给策略里的条件表达式用。
    分两处收：查询参数、路径变量。*/
    private Map<String, Object> buildParams(HttpServletRequest request) {
        Map<String, Object> params = new HashMap<>();
        /*1. 查询参数，形如 ?deptId=1 或 ?ids=1&ids=2  如果是路径参数会自动跳过*/

        //request.getParameterMap().entrySet()把请求的参数封装成Map键值对，再转为集合用于遍历
        for (Map.Entry<String, String[]> entry : request.getParameterMap().entrySet()) {
            //定义字符串数组 values 接收 entry.getValue()，即请求参数的值部分。
            String[] values = entry.getValue();
            //判断例外，免得下一行取值越界
            if (values == null || values.length == 0) {
                continue;
            }
            //往params中存入键值对，单值转Integer，多值直接存数组
            params.put(entry.getKey(), values.length == 1 ? toNumberIfPossible(values[0]) : values);
        }


        // 2. 路径变量，形如 /emps/{id} 里的 id

        //getAttribute()：从指定域中取值，接口中的常量URI_TEMPLATE_VARIABLES_ATTRIBUTE为存放路径变量的Map
        Object uriTemplateVariables = request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
        //如果是Map类型就强转Map类型的uriVars然后执行for循环，不是就直接返回
        if (uriTemplateVariables instanceof Map<?, ?> uriVars) {
            //uriVars是Map类型，转成集合然后遍历它
            for (Map.Entry<?, ?> entry : uriVars.entrySet()) {
                //这里 key 的声明类型是Object，统一转成 String 再放进去，否则 Map 里会出现非字符串的键，策略里没法引用
                params.put(String.valueOf(entry.getKey()), entry.getValue());
            }
        }
        return params;
    }


    //纯数字的字符串转成 Integer，其它原样返回。
    private Object toNumberIfPossible(String value) {
        //不为空且为纯数字字符串纯数字的字符串
        if (value != null && value.matches("-?\\d+")) {
            try {
                //转换成Integer类型
                return Integer.valueOf(value);
                //超出 int 范围的大数字保持字符串。
            } catch (NumberFormatException e) {
                return value;
            }
        }
        return value;
    }
}
