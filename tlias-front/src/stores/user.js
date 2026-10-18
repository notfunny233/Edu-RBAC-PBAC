import { defineStore } from 'pinia'
import { ref } from 'vue'

// 登录信息（token + 用户）存这里。
// 存在 localStorage 里，刷新页面后还在，不用重新登录。
// 改过键名：原来的 tlias_token/tlias_user 是教程留下的，F12 一看就露。
// 老键名不清理，避免影响正在登录的会话。
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
