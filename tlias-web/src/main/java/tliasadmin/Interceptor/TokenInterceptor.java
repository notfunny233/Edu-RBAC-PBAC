package tliasadmin.Interceptor;

import tliasadmin.Mapper.EmpMapper;
import tliasadmin.pojo.Emp;
import tliasadmin.utils.JwtUtils;
import tliasadmin.utils.LoginContextHolder;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 登录鉴权拦截器
 * <p>
 * 职责：校验请求头里的 token 是否合法，并把"当前登录人"放进 LoginContextHolder。
 * <p>
 * 权限校验（PermissionInterceptor）依赖它先跑：没有登录人就无从判断权限。
 * 所以 Webconfig 里注册顺序是 TokenInterceptor 在前、PermissionInterceptor 在后。
 */
@Component
@Slf4j
public class TokenInterceptor implements HandlerInterceptor {

    @Autowired
    private EmpMapper empMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        //获取请求的URL如果是login直接放行
        String requestURI = request.getRequestURI();
        if(requestURI.contains("login")){
            log.info("登录请求");
            return true;
        }
        //获取请求头中的Token
        String token = request.getHeader("token");

        //检验token是否为空，空则返回401
        if(token==null||token.isEmpty()){
            log.info("token为空");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);//401
            return false;
        }
        //检验token，返回结果
        Claims claims;
        try {
            claims = JwtUtils.parseToken(token);
        } catch (Exception e) {
            log.info("非法令牌");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);//401
            return false;
        }

        //令牌合法，把登录人放进上下文，供后面的权限校验和操作日志取用
        //为什么在这里查一次库，而不是把部门、岗位这些信息也塞进 JWT：
        //  1) JWT 只是签名、没有加密，客户端能看到全部载荷，塞组织信息等于把内部结构暴露出去
        //  2) 令牌一旦签发就改不了，员工调岗/换部门后旧令牌里还是老数据，权限判断会出错
        //代价是每个请求多一次查询；量级上来后应该给这一层加缓存(Redis 或本地缓存)
        Object idObj = claims.get("id");
        if (idObj == null) {
            log.info("令牌中没有员工ID");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);//401
            return false;
        }
        //为什么用 ((Number) xxx).intValue() 而不是直接 (Integer) 强转：
        //JWT 里的数字是 JSON 反序列化出来的，小整数落到 Integer、大整数可能落到 Long，
        //直接强转 Integer 在边界值上会抛 ClassCastException
        Integer empId = ((Number) idObj).intValue();
        Emp emp = empMapper.getempById(empId);
        if (emp == null) {
            //令牌签名有效，但这个人已经被删掉了
            log.info("令牌对应的员工不存在, empId:{}", empId);
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);//401
            return false;
        }
        LoginContextHolder.setEmp(emp);//把当前登录人放进ThreadLocal

        //通过所有检测且不为login请求
        //放行
        log.info("放行, 操作人:{}", emp.getName());
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        LoginContextHolder.clear();
    }

}
