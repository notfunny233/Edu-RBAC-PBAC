package tliasadmin.Service.Impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import tliasadmin.Mapper.EmpExprMapper;
import tliasadmin.Mapper.EmpLogMapper;
import tliasadmin.Mapper.EmpMapper;
import tliasadmin.Mapper.EmpRoleMapper;
import tliasadmin.Service.EmpLogService;
import tliasadmin.Service.EmpService;
import tliasadmin.pojo.*;
import tliasadmin.utils.JwtUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class EmpServiceImpl implements EmpService {

    @Autowired
    private EmpMapper empMapper;
    @Autowired
    private EmpExprMapper empExprMapper;
    @Autowired
    private EmpLogService empLogService;
    @Autowired
    private EmpLogMapper empLogMapper;
    @Autowired
    private EmpRoleMapper empRoleMapper;

    @Override
    public PageResult<Emp> page(EmpQueryParam empQueryParam) {
    //-------------------------------------------------------原始方法-----------------------------------------------------------------

        /*//调用Mapper接口返回总数据数
        Long total=empMapper.count();

        //调用Mapper接口返回分页查询的结果
        Integer start =(page-1)*pageSize;
        List<Emp> rows = empMapper.list(start, pageSize);

        //封装成对象
        return new PageResult<Emp>(total,rows);*/
    //-------------------------------------------------------原始方法-----------------------------------------------------------------
        //设置分页参数
        PageHelper.startPage(empQueryParam.getPage(),empQueryParam.getPageSize());

        //调用Mapper接口
        List<Emp> rows = empMapper.list(empQueryParam);

        //封装成对象
        PageInfo<Emp> p = new PageInfo<Emp>(rows) ;
        return new PageResult<Emp>(p.getTotal(), p.getList());

    }
    @Transactional(rollbackFor = Exception.class)//事务管理
    @Override
    public void save(Emp emp) {
        try {
            //员工信息
            emp.setCreateTime(LocalDateTime.now());
            emp.setUpdateTime(LocalDateTime.now());
            empMapper.empinfoinsert(emp);


            //员工工作经历// 判断集合exprList不为null，并且不是空集合
            List<EmpExpr> exprList = emp.getExprList();
            if(!CollectionUtils.isEmpty(exprList)){
               // 如果里面不为空，先把传递主键再执行批量插入工作经历
                exprList.forEach(EmpExpr -> {
                    EmpExpr.setEmpId(emp.getId());
                });
                empExprMapper.empexprinsert(exprList);
            }
        } finally {//无论是否异常都执行
            //记录日志
            EmpLog emplog=new EmpLog(null,LocalDateTime.now(),"新增员工：+"+emp);
            empLogService.insertLog(emplog);

        }


    }

    @Transactional(rollbackFor = {Exception.class})//基本信息和工作经历具有统一性必须同删同存，用事务包裹
    @Override
    public void del(List<Integer> ids) {
        //批量删除员工
        empMapper.del(ids);
        //批量删除员工工作经历
        empExprMapper.delep(ids);
        //批量删除员工角色关联（权限模块接入）
        //这一句是安全相关的，不只是清理数据：emp_role 里如果留着指向已删员工的角色关联，
        //将来新增员工复用了同一个自增ID，这个人一登录就自动继承了前任的权限。
        //这种越权不会报错、也不会有人察觉，必须靠删除时同步清理来避免
        empRoleMapper.deleteByEmpIds(ids);
    }
    @Override
    public Emp getempById(Integer id) {
        Emp emp = empMapper.getempById(id);
        return emp;
    }

    @Transactional(rollbackFor = {Exception.class})
    @Override
    public void update(Emp emp) {
        //更新员工基本信息
        empMapper.empupdate(emp);
        //更新员工工作经历
        //先删除原有工作经历
        empExprMapper.delep(Arrays.asList(emp.getId()));
        //在添加新的工作经历
        //需要先赋值EmpId
        List<EmpExpr> exprList = emp.getExprList();
        if(!CollectionUtils.isEmpty(exprList))//工作经历不为空
        {
            //遍历集合并为每个元素的EmpId赋值
            exprList.forEach(empExpr -> empExpr.setEmpId(emp.getId()));
            //新增
            empExprMapper.empexprinsert(exprList);
        }

    }

    @Override
    public Logininfo login(Emp emp) {
        Emp e = empMapper.selectusernameandpassword(emp);
        if (e != null) //登录成功
        {
            //创建JWT令牌
            Map<String, Object> claim=new HashMap<>();
            claim.put("id", e.getId());
            claim.put("username", e.getUsername());
            String token = JwtUtils.generateToken(claim);
            return new Logininfo(e.getId(), e.getUsername(), token);
        }
        return null;
    }

    @Override
    public List<Emp> findall() {
        List<Emp> list =empExprMapper.selectall();
        return list;

    }

}

