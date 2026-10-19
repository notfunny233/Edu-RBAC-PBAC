<script setup>
import { ref, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/stores/user'
import Sidebar from './Sidebar.vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

// 侧边栏是否收起。收起后只留 64px 宽的图标条。
const collapse = ref(false)

// 顶栏中间显示当前页面的名字，取值来自路由的 meta.title
const currentTitle = computed(() => route.meta.title || '')

const handleCommand = async (command) => {
  if (command !== 'logout') return

  // 退出前问一句，避免手滑点错
  try {
    await ElMessageBox.confirm('确定要退出登录吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
  } catch {
    // 用户点了取消，什么都不做
    return
  }

  userStore.logout()
  ElMessage.success('已退出登录')
  router.replace('/login')
}
</script>

<template>
  <el-container class="layout">
    <el-aside class="layout-aside" :width="collapse ? '64px' : '200px'">
      <div class="logo">{{ collapse ? '教' : '教务管理系统' }}</div>
      <Sidebar :collapse="collapse" />
    </el-aside>

    <el-container>
      <el-header class="layout-header">
        <div class="header-left">
          <el-icon class="collapse-btn" @click="collapse = !collapse">
            <Fold v-if="!collapse" />
            <Expand v-else />
          </el-icon>
          <span class="page-title">{{ currentTitle }}</span>
        </div>

        <div class="header-right">
          <el-dropdown @command="handleCommand">
            <span class="user-info">
              <el-icon><UserFilled /></el-icon>
              <span class="username">{{ userStore.userInfo.username || '未登录' }}</span>
              <el-icon><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="logout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <el-main class="layout-main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<style scoped>
.layout {
  height: 100vh;
}

.layout-aside {
  display: flex;
  flex-direction: column;
  background-color: #2b3648;
  transition: width 0.2s;
  overflow: hidden;
}

.logo {
  height: 60px;
  line-height: 60px;
  text-align: center;
  color: #fff;
  font-size: 16px;
  font-weight: 500;
  white-space: nowrap;
  letter-spacing: 1px;
}

.layout-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 60px;
  padding: 0 20px;
  background-color: #fff;
  border-bottom: 1px solid #ebeef5;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 14px;
}

.collapse-btn {
  font-size: 20px;
  color: #606266;
  cursor: pointer;
}

.collapse-btn:hover {
  color: #409eff;
}

.page-title {
  font-size: 16px;
  color: #303133;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 6px;
  color: #606266;
  cursor: pointer;
  outline: none;
}

.username {
  font-size: 14px;
}

.layout-main {
  background-color: #f5f7fa;
  padding: 20px;
  overflow-y: auto;
}
</style>
