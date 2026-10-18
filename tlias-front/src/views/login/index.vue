<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { login } from '@/api/user'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()

const formRef = ref()
const loading = ref(false)

const form = reactive({
  username: '',
  password: ''
})

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

// 学习用的测试账号，点一下自动填进表单，省得每次手打
const testAccounts = [
  { label: '超级管理员', username: 'shinaian' },
  { label: '班主任', username: 'songjiang' },
  { label: '讲师', username: 'lujunyi' }
]

function fillAccount(account) {
  form.username = account.username
  form.password = '123456'
}

const handleLogin = async () => {
  // 先校验表单。没通过会抛出来，这里直接中断，不发请求
  try {
    await formRef.value.validate()
  } catch {
    return
  }

  loading.value = true
  try {
    const res = await login({ ...form })

    // 拦截器已经确认过 code === 1 才会走到这里，
    // 所以 res.data 就是登录信息：{ id, username, token }
    userStore.setLoginInfo(res.data)

    ElMessage.success('登录成功')
    router.replace('/dashboard')
  } catch {
    // 提示已经在 response 拦截器里弹过了，这里不用重复弹
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="login-page">
    <div class="login-box">
      <div class="login-title">教务管理系统</div>
      <div class="login-subtitle">请登录后使用</div>

      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        size="large"
        @keyup.enter="handleLogin"
      >
        <el-form-item prop="username">
          <el-input v-model="form.username" placeholder="用户名" clearable>
            <template #prefix>
              <el-icon><User /></el-icon>
            </template>
          </el-input>
        </el-form-item>

        <el-form-item prop="password">
          <el-input
            v-model="form.password"
            type="password"
            placeholder="密码"
            show-password
          >
            <template #prefix>
              <el-icon><Lock /></el-icon>
            </template>
          </el-input>
        </el-form-item>

        <el-form-item>
          <el-button
            type="primary"
            class="login-btn"
            :loading="loading"
            @click="handleLogin"
          >
            登 录
          </el-button>
        </el-form-item>
      </el-form>

      <div class="test-accounts">
        <span class="tip">测试账号（点一下自动填入，密码都是 123456）：</span>
        <el-tag
          v-for="item in testAccounts"
          :key="item.username"
          class="account-tag"
          @click="fillAccount(item)"
        >
          {{ item.label }}：{{ item.username }}
        </el-tag>
      </div>
    </div>
  </div>
</template>

<style scoped>
.login-page {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100vh;
  background-color: #eef1f6;
}

.login-box {
  width: 400px;
  padding: 40px 36px 28px;
  background-color: #fff;
  border-radius: 12px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
}

.login-title {
  font-size: 22px;
  font-weight: 500;
  color: #303133;
  text-align: center;
}

.login-subtitle {
  margin: 8px 0 28px;
  font-size: 13px;
  color: #909399;
  text-align: center;
}

.login-btn {
  width: 100%;
}

.test-accounts {
  margin-top: 8px;
  padding-top: 16px;
  border-top: 1px dashed #ebeef5;
}

.tip {
  display: block;
  margin-bottom: 10px;
  font-size: 12px;
  color: #909399;
}

.account-tag {
  margin: 0 8px 8px 0;
  cursor: pointer;
}
</style>
