package eduadmin.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Date;

@Data
public class Students {
    private Integer id;
    private String name;
    private String no;
    private Integer gender;
    private String phone;
    private Integer degree;
    private String idCard;
    private Integer isCollege;
    private String address;
    private Date graduationDate;
    private Integer violationCount=0;
    private Integer violationScore=0;
    private Integer clazzId;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    // 关联查询，班级名称，数据库没有此字段，用于页面回显
    private String clazzName;

}
