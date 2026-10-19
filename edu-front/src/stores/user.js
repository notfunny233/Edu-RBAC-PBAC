import { defineStore } from 'pinia'
import { ref } from 'vue'

// 登录信息（token + 用户）存这里。
// 存在 localStorage 里，刷新页面后还在，不用重新登录。
// 键名前缀带项目标识，避免和同域名下其他应用撞车。
const TOKEN_KEY = 'edu_token'
const USER_KEY = 'edu_user'

export const useUserStore = defineStore('user', () => {
  const token = ref(localStorage.getItem(TOKEN_KEY) || '')
  const userInfo = ref(JSON.parse(localStorage.getItem(USER_KEY) || '{}'))

  // 登录成功后调用，把后端返回的信息存下来
  function setLoginInfo(data) {
    // 后端返回的 data 是：{ id, username, token }
    // 只取 id 和 username 展示用，token 单独存一份给请求拦截器。
    token.value = data.token
    userInfo.value = { id: data.id, username: data.username }

    localStorage.setItem(TOKEN_KEY, token.value)
    localStorage.setItem(USER_KEY, JSON.stringify(userInfo.value))
  }

  // 退出登录，或者 token 过期被踢时调用
  function logout() {
    token.value = ''
    userInfo.value = {}
    localStorage.removeItem(TOKEN_KEY)
    localStorage.removeItem(USER_KEY)
  }

  return { token, userInfo, setLoginInfo, logout }
})
