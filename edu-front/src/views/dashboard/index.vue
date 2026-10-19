<script setup>
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
</script>

<template>
  <div class="dashboard">
    <el-card shadow="never" class="welcome-card">
      <div class="welcome-title">
        你好，{{ userStore.userInfo.username || '访客' }}
      </div>
      <div class="welcome-sub">
        登录已经打通，token 会自动带在每个请求上。左边菜单点进去都是占位页，接下来一个个填。
      </div>
    </el-card>

    <el-card shadow="never" class="next-card">
      <template #header>
        <span>下一步做什么</span>
      </template>
      <ol class="todo-list">
        <li>先确认左侧每个菜单都能点开、刷新页面不会掉登录、退出登录能回登录页</li>
        <li>确认没问题后开始做第一个真实模块：<b>部门管理</b></li>
        <li>部门模块定下代码风格后，员工、班级、学生、角色、权限点、策略照着复制</li>
      </ol>
    </el-card>
  </div>
</template>

<style scoped>
.dashboard {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.welcome-title {
  font-size: 20px;
  font-weight: 500;
  color: #303133;
}

.welcome-sub {
  margin-top: 10px;
  font-size: 14px;
  color: #606266;
  line-height: 1.7;
}

.todo-list {
  margin: 0;
  padding-left: 20px;
  font-size: 14px;
  color: #606266;
  line-height: 2;
}

.todo-list b {
  font-weight: 500;
  color: #409eff;
}
</style>
