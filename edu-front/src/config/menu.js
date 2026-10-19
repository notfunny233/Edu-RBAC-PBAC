// 侧边菜单的配置。
//
// 这轮按老大的要求先"写死"，菜单跟着这份数组走。
// 等要做权限控制时，只需要在这里给每一项加一个 code 字段（权限点编码），
// 再拿 GET /auth/permissions 返回的编码数组过滤一遍就行，其余地方都不用动。
//
// 注意：菜单里的 path 必须和 router/index.js 里注册的路径对得上，
// 对不上点菜单就会跳到 404。加菜单时记得两边一起加。

export const menus = [
  { path: '/dashboard', title: '首页', icon: 'HomeFilled' },
  { path: '/dept', title: '部门管理', icon: 'OfficeBuilding' },
  { path: '/emp', title: '员工管理', icon: 'User' },
  { path: '/clazz', title: '班级管理', icon: 'School' },
  { path: '/student', title: '学生管理', icon: 'UserFilled' },
  {
    title: '权限管理',
    icon: 'Lock',
    children: [
      { path: '/role', title: '角色管理' },
      { path: '/permission', title: '权限点管理' },
      { path: '/policy', title: '权限策略' }
    ]
  },
  { path: '/report', title: '报表统计', icon: 'DataAnalysis' },
  { path: '/log', title: '操作日志', icon: 'Document' }
]
