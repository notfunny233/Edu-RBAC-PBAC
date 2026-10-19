import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { fileURLToPath, URL } from 'node:url'

export default defineConfig({
  plugins: [vue()],

  resolve: {
    // 用 @ 代表 src 目录，import 时就不用写一长串 ../../
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    }
  },

  server: {
    port: 5173,
    // 前端页面里所有请求都写成 /api/xxx，
    // 这里再把 /api 换掉，转发到真正的后端 8080。
    // 走代理就不会有浏览器的跨域问题，前端代码里也不用写死 http://localhost:8080
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/api/, '')
      }
    }
  }
})
