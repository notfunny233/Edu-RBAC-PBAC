package eduadmin.pojo;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

/**
 * Policy 分页 + 条件查询参数
 * 分页与查询条件合在一个类中，Controller 直接以对象接收即可
 */
@Data //lombok，生成get/set/toString
public class PolicyQueryParam {
    private Integer page = 1; // 默认页码1
    private Integer pageSize = 10; // 默认页大小10
    private String name; // 策略名称，模糊匹配
    private Integer subjectType; // 主体类型, 1:角色, 2:员工
    private String resource; // 资源, 精确匹配
    private String action; // 操作, 精确匹配
    private Integer effect; // 效果, 1:允许, 0:拒绝
    private Integer status; // 状态, 1:启用, 0:停用
    @DateTimeFormat(pattern = "yyyy-MM-dd") // 日期字符串转LocalDate
    private LocalDate begin;
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate end;
}
