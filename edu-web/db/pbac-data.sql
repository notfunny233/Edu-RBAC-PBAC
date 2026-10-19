-- ============================================================================
-- Tlias 权限体系 —— 初始数据脚本
--
-- 前置：先执行 pbac-schema.sql 建好五张表
-- 命令示例：
--   mysql -h127.0.0.1 -uroot -p1234 tlias < pbac-data.sql
--
-- 全部用 INSERT IGNORE：靠唯一键去重，重复执行不会报错也不会产生重复数据，
-- 所以数据改乱了可以直接重跑本脚本恢复。
--
-- ⚠️ 注意：开启权限校验后，如果库里没有这些数据，
--    所有标了 @RequiresPermission 的接口都会返回 403（默认拒绝），
--    所以要先跑本脚本再启动应用。
-- ============================================================================


-- ----------------------------------------------------------------------------
-- 一、角色
-- ----------------------------------------------------------------------------
insert ignore into role (name, code, remark, create_time, update_time)
values ('超级管理员', 'super_admin', '持通配权限点 *，全部接口可用', now(), now()),
       ('校区管理员', 'campus_admin', '管理本校区人员与班级，不能删部门', now(), now()),
       ('班主任', 'head_teacher', '带班老师，负责本班学员与档案', now(), now()),
       ('讲师', 'teacher', '只读查看班级与学员', now(), now());


-- ----------------------------------------------------------------------------
-- 二、权限点
--    编码统一 resource:action，resource 尽量与接口路径的第一段保持一致
--    （/emps 对应 emp，/depts 对应 dept），这样"接口 → 权限点"一眼能对上
-- ----------------------------------------------------------------------------
insert ignore into permission (name, code, type, parent_id, sort, remark, create_time, update_time)
values
    -- 通配权限点：超级管理员用
    ('全部权限', '*', 3, 0, 0, '通配权限点，持有它的主体无需再逐项授权', now(), now()),

    -- 部门
    ('查询部门', 'dept:list', 3, 0, 11, null, now(), now()),
    ('新增部门', 'dept:add', 3, 0, 12, null, now(), now()),
    ('修改部门', 'dept:update', 3, 0, 13, null, now(), now()),
    ('删除部门', 'dept:delete', 3, 0, 14, null, now(), now()),

    -- 员工
    ('查询员工', 'emp:list', 3, 0, 21, null, now(), now()),
    ('新增员工', 'emp:add', 3, 0, 22, null, now(), now()),
    ('修改员工', 'emp:update', 3, 0, 23, null, now(), now()),
    ('删除员工', 'emp:delete', 3, 0, 24, null, now(), now()),

    -- 班级
    ('查询班级', 'clazz:list', 3, 0, 31, null, now(), now()),
    ('新增班级', 'clazz:add', 3, 0, 32, null, now(), now()),
    ('修改班级', 'clazz:update', 3, 0, 33, null, now(), now()),
    ('删除班级', 'clazz:delete', 3, 0, 34, null, now(), now()),

    -- 学员
    ('查询学员', 'student:list', 3, 0, 41, null, now(), now()),
    ('新增学员', 'student:add', 3, 0, 42, null, now(), now()),
    ('修改学员', 'student:update', 3, 0, 43, null, now(), now()),
    ('删除学员', 'student:delete', 3, 0, 44, null, now(), now()),

    -- 文件上传
    ('文件上传', 'upload:file', 3, 0, 51, null, now(), now()),

    -- 角色管理
    ('查询角色', 'role:list', 3, 0, 61, null, now(), now()),
    ('新增角色', 'role:add', 3, 0, 62, null, now(), now()),
    ('修改角色', 'role:update', 3, 0, 63, null, now(), now()),
    ('删除角色', 'role:delete', 3, 0, 64, null, now(), now()),
    ('角色授权', 'role:assign', 3, 0, 65, '给角色分配权限点、给员工分配角色', now(), now()),

    -- 权限点管理
    ('查询权限点', 'permission:list', 3, 0, 71, null, now(), now()),
    ('新增权限点', 'permission:add', 3, 0, 72, null, now(), now()),
    ('修改权限点', 'permission:update', 3, 0, 73, null, now(), now()),
    ('删除权限点', 'permission:delete', 3, 0, 74, null, now(), now()),

    -- 策略管理
    ('查询策略', 'policy:list', 3, 0, 81, null, now(), now()),
    ('新增策略', 'policy:add', 3, 0, 82, null, now(), now()),
    ('修改策略', 'policy:update', 3, 0, 83, null, now(), now()),
    ('删除策略', 'policy:delete', 3, 0, 84, '能删策略等于能绕开策略，这条权限要谨慎授予', now(), now()),

    -- 统计报表
    ('查询统计报表', 'report:list', 3, 0, 85, '员工职位/性别分布、班级学员数等聚合数据', now(), now()),

    -- 操作日志
    ('查询操作日志', 'log:list', 3, 0, 91, null, now(), now());


-- ----------------------------------------------------------------------------
-- 三、给角色授权限点
--    用 select 子查询按 code 关联取 id，避免写死自增主键（导入顺序不同也不会错）
-- ----------------------------------------------------------------------------
-- 1) 超级管理员：通配权限点 *
insert ignore into role_permission (role_id, permission_id)
select r.id, p.id
from role r,
     permission p
where r.code = 'super_admin'
  and p.code = '*';

-- 2) 校区管理员：各模块查改 + 有限新增，不给删部门/删员工
insert ignore into role_permission (role_id, permission_id)
select r.id, p.id
from role r,
     permission p
where r.code = 'campus_admin'
  and p.code in ('dept:list',
                 'emp:list', 'emp:add', 'emp:update',
                 'clazz:list', 'clazz:add', 'clazz:update',
                 'student:list', 'student:add', 'student:update',
                 'upload:file',
                 'role:list',
                 'report:list',
                 'log:list');

-- 3) 班主任：能管本班学员，能看部门/员工/班级列表
insert ignore into role_permission (role_id, permission_id)
select r.id, p.id
from role r,
     permission p
where r.code = 'head_teacher'
  and p.code in ('dept:list',
                 'emp:list',
                 'clazz:list',
                 'student:list', 'student:add', 'student:update', 'student:delete',
                 'upload:file');

-- 4) 讲师：只读
insert ignore into role_permission (role_id, permission_id)
select r.id, p.id
from role r,
     permission p
where r.code = 'teacher'
  and p.code in ('clazz:list', 'student:list');


-- ----------------------------------------------------------------------------
-- 四、给员工分配角色
--    主体直接复用 emp 表，不新建 user 表 —— 登录链路（/login 校验 emp.username
--    + emp.password）完全不用改，登录人的 empId 就是这里的主体 ID
-- ----------------------------------------------------------------------------
insert ignore into emp_role (emp_id, role_id)
select e.id, r.id
from emp e,
     role r
where e.username = 'shinaian'   -- 施耐庵 / 123456，教研主管
  and r.code = 'super_admin';

insert ignore into emp_role (emp_id, role_id)
select e.id, r.id
from emp e,
     role r
where e.username = 'songjiang'  -- 宋江 / 123456，讲师岗，用于演示策略拦截
  and r.code = 'head_teacher';

insert ignore into emp_role (emp_id, role_id)
select e.id, r.id
from emp e,
     role r
where e.username = 'lujunyi'    -- 卢俊义 / 123456，只读查看用
  and r.code = 'teacher';


-- ----------------------------------------------------------------------------
-- 五、策略（PBAC 的核心，RBAC 里没有这一层）
--
--    condition_expr 是 SpEL 表达式，可以用的变量：
--      #emp      当前登录人（可以读 deptId / job / name 等属性）
--      #params   本次请求参数（路径变量 + 查询参数，纯数字已经转成 Integer）
--
--    也可以写 #params.deptId 来取 Map 里的值，等价于 #params['deptId']
--
--    下面四条策略覆盖三种典型用法：
--      ① 无条件拒绝   ② 看登录人属性   ③ 看请求参数   ④ 用优先级覆盖
-- ----------------------------------------------------------------------------
insert ignore into policy (name, subject_type, subject_id, resource, action, effect, condition_expr, priority, status,
                           remark, create_time, update_time)
select '班主任不得删除部门',
       1, r.id, 'dept', 'delete', 0, null, 10, 1,
       '无条件拒绝：条件为空表示一直生效。部门是组织架构，删除影响大，不放开给班主任。',
       now(), now()
from role r
where r.code = 'head_teacher';

insert ignore into policy (name, subject_type, subject_id, resource, action, effect, condition_expr, priority, status,
                           remark, create_time, update_time)
select '非学工主管不得修改员工信息',
       1, r.id, 'emp', 'update', 0, '#emp.job != 3', 20, 1,
       '看登录人属性：job=3 是学工主管。不是学工主管就不许改员工信息。',
       now(), now()
from role r
where r.code = 'head_teacher';

insert ignore into policy (name, subject_type, subject_id, resource, action, effect, condition_expr, priority, status,
                           remark, create_time, update_time)
select '班主任仅可查询本部门员工',
       1, r.id, 'emp', 'list', 0, '#params.deptId != null and #emp.deptId != #params.deptId', 30, 1,
       '看请求参数：最常见的数据权限做法。传了 deptId 且不是自己部门才拦截；不传 deptId 就不管，否则会把不带条件的列表查询也挡掉。',
       now(), now()
from role r
where r.code = 'head_teacher';

insert ignore into policy (name, subject_type, subject_id, resource, action, effect, condition_expr, priority, status,
                           remark, create_time, update_time)
select '班主任查询本部门员工显式放行',
       1, r.id, 'emp', 'list', 1, '#params.deptId != null and #params.deptId == #emp.deptId', 50, 1,
       '用优先级覆盖：priority=50 高于上面那条拒绝策略的 30，所以查本部门时会先命中这条，直接放行。想看优先级的效果，可以把条件改成 true 再去跨部门查一次。',
       now(), now()
from role r
where r.code = 'head_teacher';
