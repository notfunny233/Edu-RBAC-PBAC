package eduadmin.Config;

import eduadmin.Interceptor.PermissionInterceptor;
import eduadmin.Interceptor.TokenInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 注册拦截器的配置类。
 * <p>
 * 提醒：这个类之前整个被注释掉了，导致 TokenInterceptor 从来没有注册过。
 * 校验令牌的代码一直都在，但没有任何地方调用它，等于所有接口都没有做登录校验。
 * 以后遇到"拦截器好像没生效"的问题，先来这里确认 addInterceptors 有没有被调用，
 * 不要只盯着拦截器自己的 preHandle 找原因。
 */
@Configuration//标识这是配置类，底层封装了@Component
public class Webconfig implements WebMvcConfigurer {

    @Autowired
    private TokenInterceptor tokenInterceptor;//注册拦截器

    @Autowired
    private PermissionInterceptor permissionInterceptor;//注入权限拦截器

    @Override
    public void addInterceptors(InterceptorRegistry registry) {

        //1. 登录鉴权：校验请求头里的 token，为空或不合法就返回 401。
        //   它排在前面，因为后面的权限校验要用它先存好的登录人
        registry.addInterceptor(tokenInterceptor).addPathPatterns("/**")
                // 登录接口本来就不带令牌，必须放行，否则永远登不进来
                .excludePathPatterns("/login");

        //2. 权限鉴权：读出方法上的 @RequiresPermission，交给决策引擎判断。
        //   ⚠️ 顺序不能反：它需要 LoginContextHolder 里已经有登录人，
        //   而登录人是 TokenInterceptor 存进去的。
        //   如果两个顺序反了，权限判定会因为取不到登录人而拒绝所有请求（表现是全站 403）
        registry.addInterceptor(permissionInterceptor).addPathPatterns("/**")
                .excludePathPatterns("/login");
    }
}

//--------以下是原来的写法，保留作对照--------
//原来整个类被注释掉，拦截器实际未生效
//@Configuration
//public class Webconfig implements WebMvcConfigurer {
//
//    @Autowired
//    private DemoInterceptor demoInterceptor;//练手用的演示拦截器
//
//    @Autowired
//    private TokenInterceptor tokenInterceptor;//注入拦截器
//
//    @Override
//    public void addInterceptors(InterceptorRegistry registry) {
//        registry.addInterceptor(tokenInterceptor).addPathPatterns("/**");//拦截所有请求
//    }
//}
