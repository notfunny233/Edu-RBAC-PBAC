import request from '@/utils/request'

// 分页 + 条件查询。参数：page、pageSize、name、subjectType、resource、action、effect、status、begin、end
//
// 这里有个和别的模块不一样的地方：name 是模糊匹配，但 resource / action 是**精确匹配**
// （后端 XML 里写的是 = 而不是 like）。所以搜"emp"能搜到"emp"，搜"em"搜不到，不是坏了。
export function pagePolicies(params) {
  return request.get('/policies', { params })
}

// 新增。请求体是 Policy 对象。
//
// 三个字段有特殊约定，写页面前要知道：
//
// 1. conditionExpr 是 SpEL 表达式（如 #emp.job != 3），后端保存时会先解析一遍语法，
//    写错当场报错并带上出错位置。**空字符串是合法的**，表示无条件生效；
//    想清空条件就传 ''，不要传 null，否则后端会当成"没改这个字段"而跳过。
//
// 2. resource / action 可以填 * 表示不限资源 / 不限操作。
//
// 3. effect 决定命中条件后的结果：1 允许、0 拒绝。
//    拒绝的优先级更高（decision引擎按 priority 从大到小取第一条命中的），
//    所以"禁止类"策略的 priority 要设得比"放行类"大。
export function addPolicy(data) {
  return request.post('/policies', data)
}

export function updatePolicy(data) {
  return request.put('/policies', data)
}

// 批量删除。策略能覆盖权限点的判断结果，删除等于"放开一条约束"，页面上要二次确认。
export function deletePolicies(ids) {
  return request.delete('/policies', {
    params: { ids },
    paramsSerializer: { indexes: null }
  })
}
