// 性别、职位这类固定选项集中放这里。
//
// 后端存的是数字：gender 1男 2女；job 1班主任 2讲师 3学工主管 4教研主管 5咨询师。
// 页面上要显示中文，就得有一份数字到中文的对应关系。
// 员工、班级、报表几个模块都会用到，所以放一处，别在每个页面里各写一遍
// （写散了以后改文案要满项目找）。

export const GENDER_OPTIONS = [
  { label: '男', value: 1 },
  { label: '女', value: 2 }
]

export const JOB_OPTIONS = [
  { label: '班主任', value: 1 },
  { label: '讲师', value: 2 },
  { label: '学工主管', value: 3 },
  { label: '教研主管', value: 4 },
  { label: '咨询师', value: 5 }
]

// 班级学科。数字是数据库里存的值。
// 这份对应关系是从 clazz.subject 那一列的注释抄下来的，以它为准
export const SUBJECT_OPTIONS = [
  { label: 'Java', value: 1 },
  { label: '前端', value: 2 },
  { label: '大数据', value: 3 },
  { label: 'Python', value: 4 },
  { label: 'Go', value: 5 },
  { label: '嵌入式', value: 6 }
]

// 学生学历。注意最后有个 6 博士，
// 后端统计学历分布的 SQL 里只写到 5，所以报表那边会少一类，不是这里写错
export const DEGREE_OPTIONS = [
  { label: '初中', value: 1 },
  { label: '高中', value: 2 },
  { label: '大专', value: 3 },
  { label: '本科', value: 4 },
  { label: '硕士', value: 5 },
  { label: '博士', value: 6 }
]

// 是否来自院校。数据库存的是 1 和 0，页面上要显示是 / 否
export const IS_COLLEGE_OPTIONS = [
  { label: '是', value: 1 },
  { label: '否', value: 0 }
]

// ==================== 权限体系相关 ====================

// 权限点类型。1 菜单 / 2 按钮 / 3 接口。
//
// 说个实话：现在库里 32 条权限点的 type 全是 3、parent_id 全是 0，
// 也就是只用了"接口"这一种，没有真正做成菜单树。
// 所以权限点页面不做树形控件——硬按 parentId 拼树会拼出一棵所有节点都平铺的假树。
// 这个字典三种都列上，以后真加了菜单权限点就能直接用
export const PERMISSION_TYPE_OPTIONS = [
  { label: '菜单', value: 1 },
  { label: '按钮', value: 2 },
  { label: '接口', value: 3 }
]

// 策略主体类型：这条策略约束的是"谁"
export const SUBJECT_TYPE_OPTIONS = [
  { label: '角色', value: 1 },
  { label: '员工', value: 2 }
]

// 策略效果：条件命中之后，判定结果是允许还是拒绝
export const EFFECT_OPTIONS = [
  { label: '允许', value: 1 },
  { label: '拒绝', value: 0 }
]

// 策略状态：停用的策略不参与权限判定（相当于暂时摘掉这条规则）
export const ENABLE_STATUS_OPTIONS = [
  { label: '启用', value: 1 },
  { label: '停用', value: 0 }
]

// 权限点编码里"资源"那一段的中文名。
// 编码约定是 resource:action（如 emp:delete），授权弹窗按资源分组显示，
// 这里负责把 emp 翻译成"员工"。查不到就原样显示编码，不会变成空白
export const RESOURCE_LABELS = {
  dept: '部门',
  emp: '员工',
  clazz: '班级',
  student: '学员',
  upload: '文件上传',
  role: '角色',
  permission: '权限点',
  policy: '权限策略',
  report: '统计报表',
  log: '操作日志'
}

// 权限点编码里"动作"那一段的中文名，授权弹窗里给每个勾选框做说明用
export const ACTION_LABELS = {
  list: '查询',
  add: '新增',
  update: '修改',
  delete: '删除',
  assign: '授权',
  file: '上传'
}

// 策略里能填的资源和操作。和权限点编码用的是同一套词，所以共用上面两份中文名。
// 页面上的下拉框开了 allow-create，想填别的可以自己敲；
// * 表示不限，列在最前面是因为它最常用
export const POLICY_RESOURCE_OPTIONS = [
  '*',
  'dept',
  'emp',
  'clazz',
  'student',
  'upload',
  'role',
  'permission',
  'policy',
  'report',
  'log'
]

export const POLICY_ACTION_OPTIONS = ['*', 'list', 'add', 'update', 'delete', 'assign', 'file']

// 按数字找中文名。
// 找不到就返回 '-'，不要让表格里出现 undefined
export function labelOf(options, value) {
  if (value === null || value === undefined) return '-'
  const hit = options.find((item) => item.value === value)
  return hit ? hit.label : '-'
}
