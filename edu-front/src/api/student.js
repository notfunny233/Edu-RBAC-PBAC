import request from '@/utils/request'

// 分页 + 条件查询。参数走 Query 字符串：page、pageSize、name、degree、clazzId
// 后端只支持这三个筛选条件（姓名、学历、班级），没有性别也没有日期范围，
// 所以页面上别做那两个控件，做了也没用
export function pageStudents(params) {
  return request.get('/students', { params })
}

// 后端还有一个 GET /students/{id} 查单个学生，
// 但列表接口用的是 select s.*，编辑要用的字段一个不少，
// 编辑时直接拿表格那一行数据填表单即可，所以这里没封装它

// 新增学生。
//
// 提交前要把 violationCount 和 violationScore 从请求体里去掉。
// 因为后端 INSERT 语句把这两列写死了，一旦传了 null 进去，
// 数据库这两列是 NOT NULL，会直接报 "cannot be null"。
// 实体类里它们的默认值就是 0，前端不传反而正好。
//
// 另外学号、手机号、身份证在数据库都有唯一约束，重复会报 SQL 异常。
export function addStudent(data) {
  return request.post('/students', data)
}

// 修改学生。同样不要把违纪字段传上去，理由和新增一样：
// 后端更新语句里写的是 <if test="violationCount!=0">，
// 意思是"值为 0 就跳过这一列"。所以传 0 也改不动它，还会让人误以为改成功了。
// 违纪次数和扣分只通过下面的扣分接口变动。
export function updateStudent(data) {
  return request.put('/students', data)
}

// 批量删除。
// 这个模块的删除和员工、部门都不一样：id 拼在**路径**里、用逗号分隔，
// 不是 ?ids=1&ids=2 那种 Query 参数。单个删除也走这个接口，传一个 id 就行。
export function deleteStudents(ids) {
  return request.delete(`/students/${ids.join(',')}`)
}

// 违纪扣分。分数也拼在路径里。
// 后端一条 SQL 干两件事：分数够就减、不够就直接归零，同时违纪次数 +1。
// 前端不用自己算。
export function deductScore(id, score) {
  return request.put(`/students/violation/${id}/${score}`)
}
