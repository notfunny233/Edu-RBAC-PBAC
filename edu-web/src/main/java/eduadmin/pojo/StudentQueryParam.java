package eduadmin.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class StudentQueryParam {

    private String name;
    private Integer degree;
    private Integer clazzId;
    private Integer page=1;
    private Integer pageSize=10;
}
