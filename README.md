# 介绍
  一个教务管理后台，覆盖部门、员工、班级、学生四条业务线，外加一套自己实现的权限体系。
权限是这个项目的主要部分：没有用 Spring Security，接口级的访问控制靠自定义注解，数据级的限制交给策略表达式，判定入口只有一个决策方法。
#技术栈
后端 Java 17、Spring Boot 4.0.8、MyBatis、PageHelper、JJWT，数据库 MySQL 8。
前端 Vue 3、Vite、Element Plus、Pinia、Vue Router、ECharts。
# 功能
  部门、员工、班级、学生四个模块，都是带条件查询和分页的增删改查。员工模块额外挂了工作经历（一对多，新增员工时一起写）和头像上传。
头像上传走阿里云 OSS，application.yaml 里的 OSS 配置需要自己填，没填的话上传接口会失败，其他功能不受影响。
报表页四个统计：员工职位分布、员工性别分布、班级人数、学生学历分布。
部门、角色、权限点、策略这几个模块的写操作会记进 operate_log，由 AOP 切面写入，包含操作人、类名方法名、入参、返回值、耗时。员工、班级、学生模块目前还没贴日志注解。
权限部分三个管理页：角色、权限点、权限策略。角色页里可以直接给某个角色勾选权限点。
# 启动
先说清楚一件事：业务表（dept、emp、clazz、student 等）的建表脚本不在仓库里，需要自己准备一个 tlias 库。权限的五张表有脚本，可以单独建。
# 1. 建权限表并灌初始数据（两个脚本按顺序执行）
mysql -uroot -p tlias < tlias-web/db/pbac-schema.sql
mysql -uroot -p tlias < tlias-web/db/pbac-data.sql

# 2. 改 tlias-web/src/main/resources/application.yaml 里的数据源地址和账号密码

# 3. 启动后端，端口 8080
cd tlias-web && mvn spring-boot:run

# 4. 启动前端，端口 5173
cd tlias-front && npm install && npm run dev
前端在 vite.config.js 里把 /api 代理到 8080 并去掉前缀，所以页面代码里写的都是 /api/xxx。
除了 /login，所有接口都要在请求头里带上登录拿到的 token，头名是小写的 token，值直接用 JWT 原文，不加 Bearer 前缀。
测试账号是 shinaian（超级管理员）、songjiang（班主任）、lujunyi（讲师），密码统一 123456，数据在 tlias-web/db/pbac-data.sql 里。
初始化数据里有 33 个权限点、4 个角色。
# 权限是怎么判的
分两层，各管一件事。
第一层是权限点，形如 dept:delete，回答"这个人能不能调这个接口"。员工通过 emp_role 关联角色，角色通过 role_permission 关联权限点。权限点支持三种写法：* 表示全部，emp:* 表示某资源下所有操作，emp:delete 是精确匹配。
第二层是策略，回答"调这个接口时有哪些额外限制"。策略里存一条 SpEL 表达式，比如 #emp.job != 3，引擎把当前登录人绑成 #emp、请求参数绑成 #params 后求值。
一个请求进来走这么一遍：TokenInterceptor 校验 JWT，把登录人查出来放进 ThreadLocal；PermissionInterceptor 读出接口方法上的 @RequiresPermission，连同请求参数交给 PolicyDecisionPoint.decide()；决策引擎先确认登录人存在，再查这个人的角色和角色下的权限点，最后过一遍策略。
判定默认是拒绝的。没分配角色拒绝，权限点对不上拒绝，策略表达式求值出错也拒绝，不会因为"算不出来"就放过去。
策略还有个开关：application.yaml 里的 pbac.enabled。权限数据配乱了导致所有人都 403 的时候，可以先关掉开关恢复系统可用，再去修数据，不用改代码重新打包。
数据库表
emp、dept、clazz、student、emp_expr 是业务表。
role、permission、emp_role、role_permission、policy 是权限表。
operate_log 记操作日志，emp_log 记更早版本的简单日志。
权限表的建表语句在 tlias-web/db/pbac-schema.sql，字段注释写在脚本里。
