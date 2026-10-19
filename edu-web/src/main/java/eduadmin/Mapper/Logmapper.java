package eduadmin.Mapper;


import eduadmin.pojo.OperateLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface Logmapper {

    //按 id 倒序，让最新的日志排在最前面。
    //原先是 select * 不带排序，MySQL 按主键升序返回，
    //结果是第一页永远是最旧的记录——刚做完的操作要翻到最后一页才看得到，
    //看起来就像"日志没记上"。用 id 而不是 operate_time 排序，
    //是因为自增主键必然反映插入先后，且 operate_time 允许为空
    @Select("select * from operate_log order by id desc")
    List<OperateLog> loglist();
}
