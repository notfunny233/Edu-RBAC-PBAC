import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/stores/user'
import Layout from '@/layout/index.vue'

// 这一轮先把所有模块的路由都占上位置，页面统一用占位组件。
// 等做某个模块的真实页面时，把 component 换成它自己的文件即可，比如：
//   component: () => import('@/views/dept/index.vue')
// 其余地方（菜单、跳转）都不用动。
const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login/index.vue'),
    meta: { title: '登录' }
  },
  {
    path: '/',
    component: Layout,
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/dashboard/index.vue'),
        meta: { title: '首页' }
      },
      {
        path: 'dept',
        name: 'Dept',
        component: () => import('@/views/dept/index.vue'),
        meta: { title: '部门管理' }
      },
      {
        path: 'emp',
        name: 'Emp',
        component: () => import('@/views/emp/index.vue'),
        meta: { title: '员工管理' }
      },
      {
        path: 'clazz',
        name: 'Clazz',
        component: () => import('@/views/clazz/index.vue'),
        meta: { title: '班级管理' }
      },
      {
        path: 'student',
        name: 'Student',
        component: () => import('@/views/student/index.vue'),
        meta: { title: '学生管理' }
      },
      {
        path: 'role',
        name: 'Role',
        component: () => import('@/views/role/index.vue'),
        meta: { title: '角色管理' }
      },
      {
        path: 'permission',
        name: 'Permission',
        component: () => import('@/views/permission/index.vue'),
        meta: { title: '权限点管理' }
      },
      {
        path: 'policy',
        name: 'Policy',
        component: () => import('@/views/policy/index.vue'),
        meta: { title: '权限策略' }
      },
      {
        path: 'report',
        name: 'Report',
        component: () => import('@/views/report/index.vue'),
        meta: { title: '报表统计' }
      },
      {
        path: 'log',
        name: 'Log',
        component: () => import('@/views/log/index.vue'),
        meta: { title: '操作日志' }
      }
    ]
  },
  {
    // 兜底：上面都没匹配上就显示 404
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: () => import('@/views/404.vue'),
    meta: { title: '页面不存在' }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 路由守卫：没登录就别想进内部页面。
// 这是"体面"层面的拦截——真正的安全靠后端拦，前端拦只是为了别让用户看到一堆报错。
router.beforeEach((to, from, next) => {
  const userStore = useUserStore()

  // 登录页永远放行，否则会出现"没登录 → 跳登录 → 又被拦 → 死循环"
  if (to.path === '/login') {
    // 已经登录了还去登录页，就直接送回首页
    if (userStore.token) {
      next('/dashboard')
    } else {
      next()
    }
    return
  }

  if (!userStore.token) {
    next('/login')
    return
  }

  next()
})

// 顺带把浏览器标签页的标题改掉，不然所有页面都叫一个名字，开多了分不清
router.afterEach((to) => {
  document.title = to.meta.title ? `${to.meta.title} - 教务管理系统` : '教务管理系统'
})

export default router
