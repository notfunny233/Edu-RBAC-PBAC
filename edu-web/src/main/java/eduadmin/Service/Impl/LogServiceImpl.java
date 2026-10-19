package eduadmin.Service.Impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import eduadmin.Mapper.Logmapper;
import eduadmin.Service.LogService;
import eduadmin.pojo.LogQueryParam;
import eduadmin.pojo.OperateLog;
import eduadmin.pojo.PageResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class LogServiceImpl implements LogService {

    @Autowired
    private Logmapper logmapper;

    @Override
    public PageResult<OperateLog> Page(LogQueryParam logQueryParam) {

        PageHelper.startPage(logQueryParam.getPage(),logQueryParam.getPagesize());

        List<OperateLog> loglist = logmapper.loglist();

        PageInfo<OperateLog> logpageInfo = new PageInfo<OperateLog>(loglist);

        return new PageResult<OperateLog>(logpageInfo.getTotal(),logpageInfo.getList());




    }
}
