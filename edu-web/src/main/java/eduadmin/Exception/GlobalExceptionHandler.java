package eduadmin.Exception;

import eduadmin.pojo.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice//标识这个类是全局异常处理器，对所有@RestController控制器生效。
public class GlobalExceptionHandler {

    /**
     * 业务异常处理（权限模块新增）
     * <p>
     * 像"角色编码已存在""超级管理员角色不允许删除"这类错误，是用户输入不满足业务规则，
     * 不是系统故障，所以把异常里的提示语原样返回，用户看到就知道该怎么改。
     * <p>
     * 为什么不用操心它和下面通配处理器的先后顺序：
     * Spring 是按"异常类型最匹配"来挑处理方法的，BusinessException 比 Exception 更具体，
     * 所以只要抛的是 BusinessException，就一定命中这个方法而不是下面那个。
     */
    @ExceptionHandler(BusinessException.class)
    public Result handleBusinessException(BusinessException e) {
        log.info("业务异常,{}", e.getMessage());
        return Result.error(e.getMessage());
    }

    @ExceptionHandler//异常处理的方法
    public Result handleException(Exception e){//Exception所有异常
        log.error("全局异常处理器，拦截到异常", e);
        return Result.error("对不起,服务器异常,请稍后重试");
    }
}
