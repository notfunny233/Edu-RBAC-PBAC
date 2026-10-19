import request from '@/utils/request'

// 分页 + 条件查询。参数走 Query 字符串（不是 JSON body），所以用 params 传。
// 参数名：page、pageSize、name、begin、end
//
// 这个模块有两处跟别的模块不一样，写页面前先看这两条：
//
// 1. pageSize 的默认值是 5，不是 10。前后端都不传时后端只返回 5 条，
//    页面上会显示"一共 5 条"，看着像数据丢了。所以页面上要显式传 pageSize。
//
// 2. begin / end 不是"开课时间在区间内"的意思，而是
//    "开课时间 >= begin 并且 结课时间 <= end"，也就是整个班级周期都落在区间里。
//    而且这两个必须同时传，只传一个的话后端会把日期条件整个忽略掉，还不报错。
export function pageClazzs(params) {
  return request.get('/clazzs', { params })
}

// 后端还有一个 GET /clazzs/{id} 用来查单个班级，
// 但列表接口已经把编辑要用的字段全返回了（名称、教室、两个日期、masterId、subject），
// 编辑时直接拿表格那一行数据填表单就行，不用再发一次请求，所以这里没封装它

// 新增。请求体是 Clazz 对象。
// 两个要留意的点：
// - id 不用传。数据库里 id 是自增列，Java 里虽然写成 String，但照样自增。
// - 班级名称在数据库有唯一约束，重名会直接抛 SQL 异常，页面上得兜住。
export function addClazz(data) {
  return request.post('/clazzs', data)
}

// 修改。
export function updateClazz(data) {
  return request.put('/clazzs', data)
}

// 删除。id 拼在路径里（不是 Query 参数），这点和部门的写法不一样。
export function deleteClazz(id) {
  return request.delete(`/clazzs/${id}`)
}

// 查询全部班级，不分页。给"新增学生时选班级"的下拉框用。
// 路径是 /clazzs/list，跟 /clazzs/{id} 长得很像，
// 但 Spring 优先匹配写死的 list，不会走到查详情那个分支，可以放心用。
export function listAllClazzs() {
  return request.get('/clazzs/list')
}
