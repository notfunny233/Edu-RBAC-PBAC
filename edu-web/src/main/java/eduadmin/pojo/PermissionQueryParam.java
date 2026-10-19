package eduadmin.pojo;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

/**
 * Permission 分页 + 条件查询参数
 * 分页与查询条件合并在一个类中，Controller 直接以对象接收即可
 */
@Data //lombok，生成get/set/toString
public class PermissionQueryParam {
    private Integer page = 1; // 默认页码1
    private Integer pageSize = 10; // 默认页大小10
    private String name; // 权限点名称，模糊匹配
    private String code; // 权限点编码，模糊匹配
    private Integer type; // 类型, 1:菜单, 2:按钮, 3:接口
    @DateTimeFormat(pattern = "yyyy-MM-dd") // 日期字符串转LocalDate
    private LocalDate begin;
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate end;
}
