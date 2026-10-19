import request from '@/utils/request'

// 查询全部部门。
// 注意这里不分页，后端直接返回一个数组，不是 rows + total 那套。
export function listDepts() {
  return request.get('/depts')
}

// 按 id 查单个部门。
// 如果列表数据里已经有完整的一条（部门只有 id 和 name，列表里都有），
// 点编辑时直接用行数据填表单就行，不用再发一次请求。
// 这个函数留着，等以后某个模块列表字段不全、需要单独拉详情时再用。
export function getDept(id) {
  return request.get(`/depts/${id}`)
}

// 新增。请求体是 Dept 对象，只需要传 name，id 由数据库生成。
export function addDept(data) {
  return request.post('/depts', data)
}

// 修改。请求体要带 id，否则后端不知道改哪条。
export function updateDept(data) {
  return request.put('/depts', data)
}

// 删除。⚠️ 这里的 id 是 Query 参数，不是路径变量，
// 所以要用 params 传，写成 /depts?id=1，而不是 /depts/1。
export function deleteDept(id) {
  return request.delete('/depts', { params: { id } })
}
