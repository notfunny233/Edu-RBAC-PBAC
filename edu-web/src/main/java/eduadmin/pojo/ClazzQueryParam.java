package eduadmin.pojo;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Data
public class ClazzQueryParam {
    private Integer page = 1; // 默认页码1
    private Integer pageSize = 5; // 默认页大小5
    private String name;
    @DateTimeFormat(pattern = "yyyy-MM-dd") // 日期字符串转LocalDate
    private LocalDate begin;
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate end;
}
