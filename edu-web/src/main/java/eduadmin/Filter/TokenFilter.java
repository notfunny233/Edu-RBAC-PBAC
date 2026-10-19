package eduadmin.Filter;

import eduadmin.utils.JwtUtils;
import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

@Slf4j
//@WebFilter("/*")
public class TokenFilter implements Filter {
    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;

        //1. 获取到请求路径
        String path = request.getServletPath();
        //getRequestURI()：完整路径，包含项目上下文路径。
        //getServletPath()：去掉项目上下文后的路径，只拿 servlet 映射部分

        //2. 判断是否是登录请求，如果路径中包含 /login，说明是登录操作，放行
        if (path.contains("/login"))
        {
            log.info("登录请求");
            filterChain.doFilter(servletRequest, servletResponse);//放行
            return;//结束方法，不在继续执行
        }
        //3. 获取请求头中的token
        String token = request.getHeader("token");

        //4. 判断token是否存在，如果不存在，说明用户没有登录，返回错误信息(响应401状态码)
        if (token == null || token.isEmpty()) //token.isEmpty(): 判断token是否为空字符串
        {
            log.info("令牌为空");
            response.setStatus(401);
            return;
        }

        //5. 如果token存在，校验令牌，如果校验失败 -> 返回错误信息(响应401状态码)
        try {
            JwtUtils.parseToken(token);
        } catch (Exception e) {
            log.info("令牌非法");
            response.setStatus(401);
            return;
        }

        //6. 校验通过，放行
        log.info("令牌校验通过，放行");
        filterChain.doFilter(servletRequest, servletResponse);


    }
}
