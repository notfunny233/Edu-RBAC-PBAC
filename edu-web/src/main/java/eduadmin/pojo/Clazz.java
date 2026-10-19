package eduadmin.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Clazz {
    private String id;
    private String name;
    private String room;
    private String beginDate;//开课时间
    private String endDate;//结课时间
    private Integer masterId;//班主任外键
    private Integer subject;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private String status;//状态
    private String masterName;//班主任名称

}
