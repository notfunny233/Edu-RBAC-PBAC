import request from '@/utils/request'

// 分页 + 条件查询。
// 参数走的是 Query 字符串（不是 JSON body），所以用 params 传。
// 参数名：page、pageSize、name、gender、begin、end
//
// ⚠️ 日期要注意：后端 XML 里写的是 <if test="begin != null and end != null">，
// 意思是 begin 和 end **必须同时传**才会生效。只传一个的话，日期条件会被整个忽略，
// 而且不报错——你会看到"筛了日期但结果没变化"。所以页面上要么两个都传，要么都不传。
export function pageEmps(params) {
  return request.get('/emps', { params })
}

// 按 id 查详情。这个接口和别的模块不一样：
// 它会连员工的**工作经历**一起返回（exprList 数组），所以编辑回显必须用它，
// 不能像部门那样直接用表格行数据填——表格里没有工作经历。
export function getEmp(id) {
  return request.get(`/emps/${id}`)
}

// 新增。请求体是 Emp 对象，其中 exprList 是工作经历数组。
// 密码不用传：后端 INSERT 语句里根本没有 password 这一列，
// 数据库那一列有默认值 '123456'，新员工直接用这个密码登录。
export function addEmp(data) {
  return request.post('/emps', data)
}

// 修改。和工作经历一起提交，后端是"先删掉旧的工作经历，再插入新的"，整体在一个事务里。
export function updateEmp(data) {
  return request.put('/emps', data)
}

// 批量删除。
//
// ⚠️ 这里有个很容易踩的坑：后端要的是 /emps?ids=1&ids=2 这种"同名参数重复出现"的格式。
// 而 axios 默认会把数组序列化成 ids[]=1&ids[]=2，多了两个方括号，后端收不到，
// 结果是"点了删除但一条都没删掉"，还不报错。
// 下面的 indexes: null 就是告诉 axios 用重复参数的形式，别加方括号。
export function deleteEmps(ids) {
  return request.delete('/emps', {
    params: { ids },
    paramsSerializer: { indexes: null }
  })
}

// 查询全部员工。这个接口是给"新增班级时选班主任"的下拉框用的。
// 员工模块自己暂时用不到，先放着。
export function listAllEmps() {
  return request.get('/emps/list')
}
