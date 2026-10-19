import request from '@/utils/request'

// 操作日志分页查询。
//
// ⚠️ 两个和其他模块都不一样的地方：
//
// 1. 每页条数的参数名是 pagesize，**全小写**。
//    别的模块都叫 pageSize（驼峰）。写错了不会报错，后端就用它自己的默认值 10，
//    你设成 5 也不生效——属于静默失效，很难发现。
//
// 2. 这个接口**没有查询条件**，只有分页。
//    后端 Service 里压根没把参数往 Mapper 传，SQL 是写死的 select *。
//    所以日志页做不了"按操作人筛 / 按时间段筛"，页面上就不要摆那些筛选框了，
//    摆了也不会生效。
export function pageLogs(params) {
  return request.get('/log/page', { params })
}
