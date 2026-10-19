package eduadmin.Exception;

/**
 * 业务异常：用来区分"用户输入不对"和"服务端故障"
 * <p>
 * 为什么需要它：
 * 像"角色编码已存在"这种错误，是用户操作的问题，不是系统故障。
 * 如果直接抛 IllegalArgumentException 之类的 RuntimeException，
 * 会落到 GlobalExceptionHandler 的通配分支里，前端只能收到
 * "对不起,服务器异常,请稍后重试" —— 用户按提示重试一百次也不会成功，
 * 真正的出错原因还被吞掉了（排查时只能翻日志）。
 * <p>
 * 用这个异常把两类错误分开：
 * 业务异常 -> 把提示语原样返回给前端，用户知道该怎么改
 * 其他异常 -> 统一返回"服务器异常"，不暴露内部细节
 */
public class BusinessException extends RuntimeException {

    public BusinessException(String message) {
        super(message);
    }

}
