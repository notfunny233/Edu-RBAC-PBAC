package eduadmin.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class JobOption {
    private List jobList;//职位列表
    private List dataList;//职位对应的数据

}
