import request from '@/utils/request'

// 分页 + 条件查询。参数走 Query 字符串（不是 JSON body），所以用 params 传。
// 参数名：page、pageSize、name、code、begin、end
//
// 日期跟员工模块一个规矩：begin 和 end 必须同时传才会生效（后端 XML 里是
// <if test="begin != null and end != null">），只传一个的话日期条件会被整个忽略，
// 而且不报错——表现是"筛了日期但结果没变化"。
export function pageRoles(params) {
  return request.get('/roles', { params })
}

// 新增。请求体是 Role 对象（name / code / remark）。
//
// code 是唯一键，但后端 Service 层提前查了一次，会返回
// "角色编码 xxx 已存在，请换一个"这种能看懂的提示；
// 不查的话唯一键冲突抛出来的是服务器异常，前端只能看到一句"系统繁忙"。
export function addRole(data) {
  return request.post('/roles', data)
}

export function updateRole(data) {
  return request.put('/roles', data)
}

// 批量删除。
//
// ⚠️ 后端要的是 /roles?ids=1&ids=2 这种"同名参数重复出现"的格式，
// axios 默认会序列化成 ids[]=1&ids[]=2，多了方括号，后端收不到，
// 结果是"点了删除一条都没删掉"，还不报错。indexes: null 就是关掉方括号。
//
// 另外后端有保护：超级管理员角色（code = super_admin）不允许删除，
// 会返回"否则将无人能进入授权页面"的提示。
export function deleteRoles(ids) {
  return request.delete('/roles', {
    params: { ids },
    paramsSerializer: { indexes: null }
  })
}

// 查询全部角色，不分页。给"给员工分配角色"的勾选列表用。
// 路径 /roles/list 和 /roles/{id} 长得像，但 Spring 优先匹配写死的 list，可以放心用。
export function listAllRoles() {
  return request.get('/roles/list')
}
