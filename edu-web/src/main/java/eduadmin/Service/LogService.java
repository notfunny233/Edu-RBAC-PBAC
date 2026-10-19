package eduadmin.Service;

import com.github.pagehelper.PageInfo;
import eduadmin.pojo.LogQueryParam;
import eduadmin.pojo.OperateLog;
import eduadmin.pojo.PageResult;

public interface LogService {
    PageResult<OperateLog> Page(LogQueryParam logQueryParam);
}
