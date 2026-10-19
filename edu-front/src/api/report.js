import request from '@/utils/request'

// 报表四个接口都是只读的查询，参数一个都不用传。
//
// 它们的返回结构**各不相同**，不是统一的 rows/total：
//
//   empJobData      { jobList: ['班主任', ...], dataList: [6, 11, ...] }
//   empGenderData   [{ name: '男性员工', value: 25 }, ...]
//   studentDegreeData [{ name: '本科', value: 9 }, ...]
//   studentCountData  { name: ['JavaEE就业163期', ...], value: [7, 2, ...] }
//
// 两种形态的原因：职位和班级适合"横轴一类一列"，所以后端拆成两个平行数组
// （jobList/dataList、name/value）；性别和学历后端直接返回对象数组。
// 接图表的时候要按各自的形状喂数据，别照着上一个抄。

// 员工职位分布。后端返回 { jobList, dataList }
export function getEmpJobData() {
  return request.get('/report/empJobData')
}

// 员工性别分布。后端返回 [{ name, value }]
export function getEmpGenderData() {
  return request.get('/report/empGenderData')
}

// 学员学历分布。后端返回 [{ name, value }]
export function getStudentDegreeData() {
  return request.get('/report/studentDegreeData')
}

// 各班级学员人数。后端返回 { name: [...], value: [...] }
export function getStudentCountData() {
  return request.get('/report/studentCountData')
}
