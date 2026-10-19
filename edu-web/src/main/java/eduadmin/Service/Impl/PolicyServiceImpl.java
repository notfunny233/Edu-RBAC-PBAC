package eduadmin.Service.Impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import eduadmin.Exception.BusinessException;
import eduadmin.Mapper.PolicyMapper;
import eduadmin.Service.PolicyService;
import eduadmin.pojo.PageResult;
import eduadmin.pojo.Policy;
import eduadmin.pojo.PolicyQueryParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service//交给IOC容器管理
public class PolicyServiceImpl implements PolicyService {

    @Autowired
    private PolicyMapper policyMapper;

    /**
     * 只用来"试解析"条件表达式，不做求值。
     * 解析器线程安全，做成字段复用，避免每次保存都 new 一个
     */
    private final ExpressionParser expressionParser = new SpelExpressionParser();

    @Override
    public PageResult<Policy> page(PolicyQueryParam policyQueryParam) {
        //设置分页参数
        PageHelper.startPage(policyQueryParam.getPage(), policyQueryParam.getPageSize());

        //调用Mapper接口
        List<Policy> rows = policyMapper.list(policyQueryParam);

        //封装成对象
        PageInfo<Policy> pageInfo = new PageInfo<>(rows);
        return new PageResult<>(pageInfo.getTotal(), pageInfo.getList());
    }

    @Transactional(rollbackFor = {Exception.class})
    @Override
    public void save(Policy policy) {
        //1. 业务校验：策略名称唯一
        if (policyMapper.countByNameExcludeId(policy.getName(), 0) > 0) {
            throw new BusinessException("策略名称 " + policy.getName() + " 已存在，请换一个");
        }
        //2. 业务校验：条件表达式语法
        checkConditionExpr(policy.getConditionExpr());
        //3. 补全基础属性 - createTime, updateTime
        policy.setCreateTime(LocalDateTime.now());
        policy.setUpdateTime(LocalDateTime.now());
        //4. 调用Mapper接口方法插入数据
        policyMapper.insert(policy);
    }

    @Transactional(rollbackFor = {Exception.class})
    @Override
    public void del(List<Integer> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        //策略没有关联表，删除只涉及一张表，严格来说可以不加事务；
        //但保持写方法一律带事务，比"这里想一下要不要事务"更不容易漏
        policyMapper.del(ids);
    }

    @Override
    public Policy getById(Integer id) {
        Policy policy = policyMapper.getById(id);
        return policy;
    }

    @Transactional(rollbackFor = {Exception.class})
    @Override
    public void update(Policy policy) {
        //1. 业务校验：策略名称唯一，排除自己
        if (policyMapper.countByNameExcludeId(policy.getName(), policy.getId()) > 0) {
            throw new BusinessException("策略名称 " + policy.getName() + " 已被其他策略占用");
        }
        //2. 业务校验：条件表达式语法
        checkConditionExpr(policy.getConditionExpr());
        //3. 补全基础属性 - updateTime
        policy.setUpdateTime(LocalDateTime.now());
        //4. 调用Mapper接口方法更新数据
        policyMapper.update(policy);
    }

    /**
     * 校验条件表达式是否写得出来（只做语法解析，不求值）
     * <p>
     * 为什么要在保存时就拦：
     * 表达式最终是在请求进来时求值的，那里求值失败为了保证安全是按"拒绝"处理的
     * （见 PolicyDecisionPoint，fail-closed）。
     * 也就是说一条写错的策略会造成"这个接口对这批人全部 403"，
     * 而且报错信息在服务端日志里，前端只看到一个没权限的提示 ——
     * 让人第一反应怀疑权限配置，而不是怀疑表达式写错了。
     * 在这里先解析一次，语法错误当场告诉配置人"第几个字符有问题"，能省掉大量排查时间。
     */
    private void checkConditionExpr(String conditionExpr) {
        if (conditionExpr == null || conditionExpr.trim().isEmpty()) {
            return;//空条件合法，表示无条件生效
        }
        try {
            expressionParser.parseExpression(conditionExpr);
        } catch (Exception e) {
            log.info("策略条件表达式语法校验失败, expr:{}", conditionExpr);
            throw new BusinessException("条件表达式语法有误：" + e.getMessage());
        }
    }

}
