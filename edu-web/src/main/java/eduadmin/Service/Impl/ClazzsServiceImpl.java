package eduadmin.Service.Impl;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import eduadmin.Mapper.ClazzsMapper;
import eduadmin.Service.ClazzsService;
import eduadmin.pojo.Clazz;

import eduadmin.pojo.ClazzQueryParam;
import eduadmin.pojo.PageResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class ClazzsServiceImpl implements ClazzsService {
    @Autowired
    private ClazzsMapper clazzsMapper;

    @Override
    public PageResult<Clazz> getclasslist(ClazzQueryParam clazzQueryParam) {
        log.info("查询班级列表");
        PageHelper.startPage(clazzQueryParam.getPage(),clazzQueryParam.getPageSize());//配置分页默认数据

        //调用Mapper接口
        List<Clazz> rows = clazzsMapper.list(clazzQueryParam);

        // 用PageInfo包装，不要强转返回进行响应
        PageInfo<Clazz> pageInfo = new PageInfo<>(rows);
        return new PageResult<>(pageInfo.getTotal(), pageInfo.getList());
    }

    @Override
    public void deleteClazz(String id) {
        log.info("删除班级");
        clazzsMapper.deleteClazz(id);


    }

    @Override
    public void saveClazz(Clazz clazz) {
        log.info("新增班级");
        clazzsMapper.insertClazz(clazz);
    }

    @Override
    public Clazz getClazzbyID(Integer id) {
        log.info("根据ID查询班级");
        Clazz clazz = clazzsMapper.selectClazzbyid(id);
        return clazz;
    }

    @Override
    public void updateClazz(Clazz clazz) {
        log.info("修改班级");
        clazzsMapper.updateclazz(clazz);

    }

    @Override
    public List<Clazz> findall() {
        log.info("查询所有班级");
        List<Clazz> clazzlist=clazzsMapper.selectall();
        return clazzlist;
    }
}

