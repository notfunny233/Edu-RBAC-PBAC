package eduadmin.pojo;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Data //lombok，生成get/set/toString
public class EmpQueryParam {
    private Integer page = 1; // 默认页码1
    private Integer pageSize = 10; // 默认页大小10
    private String name;
    private Integer gender;
    @DateTimeFormat(pattern = "yyyy-MM-dd") // 日期字符串转LocalDate
    private LocalDate begin;
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate end;
}
