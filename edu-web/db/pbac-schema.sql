-- ============================================================================
-- Tlias 权限体系建表脚本
--
-- 用法：先执行本文件，再执行 pbac-data.sql 灌入初始数据
-- 命令示例：
--   mysql -h127.0.0.1 -uroot -p1234 tlias < pbac-schema.sql
--
-- 一共五张表，分成两部分理解：
--
-- 第一部分（前四张）是 RBAC，回答"这个人能不能调这个接口"：
--   角色、权限点，以及两张关联表
--   员工 --(emp_role)--> 角色 --(role_permission)--> 权限点
--
-- 第二部分（第五张 policy）是 PBAC，回答"在什么条件下才能调"：
--   同样是"资源 + 操作"，但多了一列条件表达式，可以限制数据范围，
--   例如"班主任只能查本部门的员工"
--
-- 所以 PBAC 不是替代 RBAC，而是加在它上面。
--
-- 命名提醒：
-- role / action / resource / effect 在 MySQL 8 里都不是保留字，可以直接当列名；
-- 但 CONDITION 是保留字，所以条件字段叫 condition_expr，避开它。
-- ============================================================================


-- ----------------------------------------------------------------------------
-- 1. 角色表
--    一个角色就是一批权限点的集合，不直接给员工挂权限点。
--    如果直接挂，27 个员工要维护 27 份授权，人员一变动就乱了。
-- ----------------------------------------------------------------------------
create table if not exists role
(
    id          int unsigned not null auto_increment comment '主键ID',
    name        varchar(32)  not null comment '角色名称, 如 班主任',
    code        varchar(64)  not null comment '角色编码, 唯一, 如 head_teacher',
    remark      varchar(255) default null comment '备注',
    create_time datetime     default null comment '创建时间',
    update_time datetime     default null comment '修改时间',
    primary key (id),
    unique key uk_role_code (code) comment '角色编码唯一, 防止重复建角色'
) comment '角色表';


-- ----------------------------------------------------------------------------
-- 2. 权限点表
--    权限点编码统一用 resource:action 两段式，例如 emp:delete。
--    这样引擎直接用冒号拆出"资源"和"操作"，不用再维护一张资源表。
--    特殊值：code = '*' 表示全部权限，给超级管理员用
-- ----------------------------------------------------------------------------
create table if not exists permission
(
    id          int unsigned not null auto_increment comment '主键ID',
    name        varchar(32)  not null comment '权限点名称, 如 删除员工',
    code        varchar(64)  not null comment '权限点编码, 格式 resource:action, 如 emp:delete; * 表示全部权限',
    type        tinyint      not null default 3 comment '类型, 1:菜单, 2:按钮, 3:接口',
    parent_id   int unsigned not null default 0 comment '父级ID, 0为顶级(菜单树用)',
    sort        int          not null default 0 comment '排序号, 越小越靠前',
    remark      varchar(255) default null comment '备注',
    create_time datetime     default null comment '创建时间',
    update_time datetime     default null comment '修改时间',
    primary key (id),
    unique key uk_permission_code (code) comment '权限点编码唯一'
) comment '权限点表';


-- ----------------------------------------------------------------------------
-- 3. 员工-角色关联表
--    主体用的是 emp 表，没有新建 user 表。
--    因为本项目登录账号本来就存在 emp 里（emp.username / emp.password），
--    复用之后登录相关的代码一行都不用改。
-- ----------------------------------------------------------------------------
create table if not exists emp_role
(
    id      int unsigned not null auto_increment comment '主键ID',
    emp_id  int unsigned not null comment '员工ID, 对应 emp.id',
    role_id int unsigned not null comment '角色ID, 对应 role.id',
    primary key (id),
    unique key uk_emp_role (emp_id, role_id) comment '同一员工不能重复绑同一角色',
    key idx_emp_role_emp (emp_id) comment '按员工查角色, 每次权限判定都要走这个索引'
) comment '员工-角色关联表';


-- ----------------------------------------------------------------------------
-- 4. 角色-权限关联表
-- ----------------------------------------------------------------------------
create table if not exists role_permission
(
    id            int unsigned not null auto_increment comment '主键ID',
    role_id       int unsigned not null comment '角色ID, 对应 role.id',
    permission_id int unsigned not null comment '权限点ID, 对应 permission.id',
    primary key (id),
    unique key uk_role_permission (role_id, permission_id) comment '同一角色不能重复绑同一权限点',
    key idx_role_permission_role (role_id) comment '按角色查权限点'
) comment '角色-权限关联表';


-- ----------------------------------------------------------------------------
-- 5. 策略表（PBAC 的核心，RBAC 里没有这张表）
--
--    一条策略的含义是：某个主体（角色或员工）对某个资源做某个操作时，
--    在 condition_expr 成立的情况下，结果是允许还是拒绝。
--
--    priority 的作用：
--    同一个资源+操作可能命中多条策略，按 priority 从大到小取第一条
--    "条件成立"的，用它的 effect 作为结果。
--    这样"高优先级的允许"就能覆盖"低优先级的拒绝"，规则冲突时有明确顺序。
--
--    condition_expr 为空 = 无条件生效（等于永远允许或永远拒绝）
-- ----------------------------------------------------------------------------
create table if not exists policy
(
    id             int unsigned not null auto_increment comment '主键ID',
    name           varchar(64)  not null comment '策略名称, 如 班主任不得删除部门',
    subject_type   tinyint      not null comment '主体类型, 1:角色, 2:员工',
    subject_id     int unsigned not null comment '主体ID, subject_type=1 时对应 role.id, =2 时对应 emp.id',
    resource       varchar(64)  not null comment '资源, 如 emp; * 表示不限资源',
    action         varchar(64)  not null comment '操作, 如 delete; * 表示不限操作',
    effect         tinyint      not null default 1 comment '效果, 1:允许, 0:拒绝',
    condition_expr varchar(512) default null comment '条件表达式(SpEL), 空表示无条件生效',
    priority       int          not null default 0 comment '优先级, 越大越先匹配',
    status         tinyint      not null default 1 comment '状态, 1:启用, 0:停用(停用后不参与判定)',
    remark         varchar(255) default null comment '备注',
    create_time    datetime     default null comment '创建时间',
    update_time    datetime     default null comment '修改时间',
    primary key (id),
    unique key uk_policy_name (name) comment '策略名称唯一：既挡住后台重复提交，也让种子脚本可以用 INSERT IGNORE 重复执行',
    key idx_policy_match (resource, action, status) comment '按资源+操作+状态过滤, 走这个联合索引'
) comment '权限策略表';

-- 为什么唯一键建在 name 上，而不是建在（主体+资源+操作）上：
-- 因为同一个主体对同一个资源+操作本来就可能有多条策略。
-- 比如班主任在 emp:list 上既有"跨部门拒绝"又有"本部门放行"两条，
-- 靠 priority 决定谁生效，所以不能拿这几个字段做唯一键。
-- 那把 condition_expr 也算进去行不行？也不行。
-- MySQL 的唯一索引不去重 NULL，而"无条件"的策略这里就是 null，仍然会被重复插入。
-- 所以用 name 唯一是最稳妥的。
