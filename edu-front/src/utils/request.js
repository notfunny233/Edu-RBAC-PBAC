import axios from 'axios'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'

// 统一出口的 axios 实例。
// 好处是：所有请求的公共逻辑只写一遍，页面里只管写业务。
//
// baseURL 写成 /api 而不是 http://localhost:8080，靠的是 vite.config.js 里的代理转发。
// 这样以后换服务器地址，只改 vite.config.js 一处，页面代码不用动。
const request = axios.create({
  baseURL: '/api',
  timeout: 10000
})

// ---------- 请求拦截器：发出去之前先夹带 token ----------
request.interceptors.request.use(
  (config) => {
    // 后端 TokenInterceptor 是从名为 token 的请求头里取值的，
    // 名字必须一字不差是小写的 token，也不是常见的 Authorization。
    // 另外它直接用 JWT 原文，不加 "Bearer " 前缀。
    const userStore = useUserStore()
    if (userStore.token) {
      config.headers.token = userStore.token
    }
    return config
  },
  (error) => Promise.reject(error)
)

// ---------- 响应拦截器：把后端的返回格式在这里统一拆掉 ----------
//
// 后端所有接口都是同一个壳：{ code, msg, data }
//   code === 1  成功，真正的数据在 data 里
//   code === 0  业务失败，失败原因在 msg 里
// 不在这里统一处理的话，每个页面都要写一遍 if (res.code === 1)，很啰嗦。
request.interceptors.response.use(
  (response) => {
    const res = response.data

    if (res.code === 1) {
      return res
    }

    // 业务失败：比如"用户名或密码错误"，这是后端正常返回的，HTTP 状态码是 200
    ElMessage.error(res.msg || '操作失败')
    return Promise.reject(new Error(res.msg || '操作失败'))
  },
  async (error) => {
    // 走到这里说明 HTTP 状态码不是 2xx，属于请求层面就失败了
    const status = error.response?.status

    if (status === 401) {
      // 401 是后端的 TokenInterceptor 返回的：没带 token，或者 token 过期了。
      // 处理办法就一个：清掉本地登录信息，踢回登录页。
      const userStore = useUserStore()
      userStore.logout()
      ElMessage.error('登录已过期，请重新登录')

      // 这里用动态 import 拿 router，而不是在文件顶部 import。
      // 因为 router 里会加载页面，页面里又会用到这个 request.js，
      // 顶部直接 import 会形成循环引用，动态导入可以避开。
      const { default: router } = await import('@/router')
      router.replace('/login')
    } else if (status === 403) {
      // 403 是权限不够。这轮还没做权限控制，但先把这个提示留好，
      // 免得以后撞上 403 只看到一句"请求失败"，不知道是自己没权限。
      ElMessage.error('没有权限执行这个操作')
    } else if (status === 404) {
      ElMessage.error('接口不存在，检查一下请求路径')
    } else {
      ElMessage.error(error.message || '网络异常，请稍后重试')
    }

    return Promise.reject(error)
  }
)

export default request
