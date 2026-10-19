import request from '@/utils/request'

// ==================== 授权相关 ====================
//
// 这个文件和别的 api 文件不是一类东西：别的文件管"业务数据"，
// 它管的是"谁能做什么"——也就是写权限。
//
// 权限体系里有两条路径，别混：
//   写（授权）：就是下面这几个 PUT 接口，改的是 emp_role / role_permission 两张关联表
//   读（校验）：后端 PolicyDecisionPoint 每次请求都会查一遍，前端看不见
//
// 所有 PUT 都是**覆盖式**语义：传的是"保存之后应该有的完整集合"，
// 后端先清空再写入（同一个事务里）。
// 所以取消全部勾选时传空数组 [] 是合法且正确的，不要因为"空数组看起来没东西"就跳过请求。
// 覆盖式还有个好处：重复提交、网络重试都不会产生重复关联。

// 查当前登录人自己的权限点编码数组，如 ['emp:list', 'emp:delete']。
//
// 后端故意没给它加权限校验，否则会出现"要先有权限才能知道自己有什么权限"的死循环。
// 持 * 的超级管理员，后端已经把它展开成完整清单了，前端不用为超管写特殊分支。
export function getCurrentPermissions() {
  return request.get('/auth/permissions')
}

// 查某个员工已分配的角色 id 数组，给"分配角色"弹窗回显勾选。
// 注意返回的是 **id 数组**（如 [2,3]），不是角色对象数组。
export function getEmpRoles(empId) {
  return request.get(`/auth/roles/${empId}`)
}

// 给员工分配角色。roleIds 是普通数组，作为 JSON 请求体发出去（不是 Query 参数）。
export function assignEmpRoles(empId, roleIds) {
  return request.put(`/auth/roles/${empId}`, roleIds)
}

// 查某个角色已分配的权限点 id 数组，给"授权"弹窗回显勾选。
export function getRolePermissions(roleId) {
  return request.get(`/auth/permissions/${roleId}`)
}

// 给角色分配权限点。这是整套权限体系里最敏感的一个接口：
// 改它等于改"谁能做什么"，所以它自己也被 role:assign 权限点保护着。
export function assignRolePermissions(roleId, permissionIds) {
  return request.put(`/auth/permissions/${roleId}`, permissionIds)
}
