package eduadmin.Mapper;


import eduadmin.pojo.Emp;
import eduadmin.pojo.EmpExpr;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface EmpExprMapper {
    //（批量）插入员工工作经历
    void empexprinsert(List<EmpExpr> exprList);
    //（批量）删除员工工作经历
    void delep(List<Integer> empids);

    //查询全部员工，用于创建班级
    @Select("select e.* from emp e where job=1")
    List<Emp> selectall();
}
