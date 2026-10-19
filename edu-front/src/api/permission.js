import request from '@/utils/request'

// 分页 + 条件查询。参数：page、pageSize、name、code、type、begin、end
// name / code 是模糊匹配，type 是精确匹配。
export function pagePermissions(params) {
  return request.get('/permissions', { params })
}

// 新增。请求体是 Permission 对象。
//
// code 有两条硬规则，后端保存时会拦：
//   1. 唯一，不能和已有权限点重复
//   2. 必须含冒号（形如 resource:action，例如 emp:delete），或者就是单独的 *
//      第 2 条为什么要在入口拦：编码写错了不会当场报错，而是"授权了但接口永远校验不通过"，
//      查起来比直接拒绝麻烦得多
export function addPermission(data) {
  return request.post('/permissions', data)
}

export function updatePermission(data) {
  return request.put('/permissions', data)
}

// 批量删除。后端要 ?ids=1&ids=2，axios 默认发成 ids[]=1，所以必须加 indexes: null。
//
// 后端有保护：通配权限点 * 不允许删除（它是超级管理员的权限来源，删了就没人能进授权页面）。
export function deletePermissions(ids) {
  return request.delete('/permissions', {
    params: { ids },
    paramsSerializer: { indexes: null }
  })
}

// 查询全部权限点，不分页。给"给角色授权限点"的勾选列表用。
export function listAllPermissions() {
  return request.get('/permissions/list')
}
