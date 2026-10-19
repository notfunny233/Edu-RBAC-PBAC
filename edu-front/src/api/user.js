import request from '@/utils/request'

// 登录。请求体是 { username, password }，后端只看这两个字段。
// 成功后返回 { code:1, msg:'success', data:{ id, username, password, token } }
export function login(data) {
  return request.post('/login', data)
}

// 查询当前登录人自己的权限点编码，比如 ['emp:list', 'emp:delete']
// 这轮（骨架阶段）还用不到，等做菜单权限时再来调它。
export function getCurrentPermissions() {
  return request.get('/auth/permissions')
}
