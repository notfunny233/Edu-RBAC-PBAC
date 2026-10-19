<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listDepts, addDept, updateDept, deleteDept } from '@/api/dept'
import { formatDateTime } from '@/utils/format'

// ============ 列表 ============

const loading = ref(false)
const deptList = ref([])

async function loadDepts() {
  loading.value = true
  try {
    const res = await listDepts()
    // 走到这里说明请求已经成功了（失败的话拦截器会拦掉并弹提示），
    // 所以直接取值就行。加个兜底是防止后端返回 data:null 时表格报错。
    deptList.value = res.data || []
  } finally {
    // 不管是成功还是失败，转圈都要停掉，否则失败时页面会一直转
    loading.value = false
  }
}

// 进页面就加载一次
onMounted(loadDepts)

// ============ 新增 / 编辑弹窗 ============
//
// 新增和编辑长得一模一样，只差一个标题和"要不要带 id"，
// 所以共用一个弹窗，用 form.id 有没有值来区分这两种情况。

const dialogVisible = ref(false)
const dialogTitle = ref('')
const submitting = ref(false)
const formRef = ref()

const form = reactive({
  id: null,
  name: ''
})

const rules = {
  name: [
    { required: true, message: '请输入部门名称', trigger: 'blur' },
    { max: 20, message: '名称最多 20 个字', trigger: 'blur' }
  ]
}

function openAdd() {
  dialogTitle.value = '新增部门'
  form.id = null
  form.name = ''
  dialogVisible.value = true
}

function openEdit(row) {
  dialogTitle.value = '编辑部门'
  // 部门只有 id 和 name 两个业务字段，列表里都已经有了，直接拿行数据填表单。
  // 省掉一次没必要的请求，点开弹窗是瞬时的，体验更好。
  form.id = row.id
  form.name = row.name
  dialogVisible.value = true
}

async function handleSubmit() {
  // 先校验。没通过会抛出来，这里直接中断，不发请求
  try {
    await formRef.value.validate()
  } catch {
    return
  }

  submitting.value = true
  try {
    if (form.id) {
      await updateDept({ id: form.id, name: form.name })
      ElMessage.success('修改成功')
    } else {
      await addDept({ name: form.name })
      ElMessage.success('新增成功')
    }

    dialogVisible.value = false
    // 改完重新拉一次列表。数据量很小，直接整体刷新最省事，
    // 不用在前端手工拼数组（那容易和数据库的真实状态对不上）。
    loadDepts()
  } finally {
    submitting.value = false
  }
}

// ============ 删除 ============

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(`确定要删除「${row.name}」吗？`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
  } catch {
    // 点了取消，什么都不做
    return
  }

  await deleteDept(row.id)
  ElMessage.success('删除成功')
  loadDepts()
}
</script>

<template>
  <el-card shadow="never">
    <div class="toolbar">
      <el-button type="primary" @click="openAdd">
        <el-icon><Plus /></el-icon>
        <span>新增部门</span>
      </el-button>
    </div>

    <el-table v-loading="loading" :data="deptList" border stripe>
      <el-table-column type="index" label="序号" width="70" align="center" />
      <el-table-column prop="name" label="部门名称" min-width="180" />
      <el-table-column label="创建时间" width="190">
        <template #default="{ row }">
          {{ formatDateTime(row.createTime) }}
        </template>
      </el-table-column>
      <el-table-column label="修改时间" width="190">
        <template #default="{ row }">
          {{ formatDateTime(row.updateTime) }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="150" align="center">
        <template #default="{ row }">
          <el-button type="primary" link @click="openEdit(row)">编辑</el-button>
          <el-button type="danger" link @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
  </el-card>

  <!-- 新增和编辑共用这一个弹窗 -->
  <el-dialog v-model="dialogVisible" :title="dialogTitle" width="420px">
    <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
      <el-form-item label="部门名称" prop="name">
        <el-input
          v-model="form.name"
          placeholder="请输入部门名称"
          maxlength="20"
          show-word-limit
          @keyup.enter="handleSubmit"
        />
      </el-form-item>
    </el-form>

    <template #footer>
      <el-button @click="dialogVisible = false">取消</el-button>
      <el-button type="primary" :loading="submitting" @click="handleSubmit">
        确定
      </el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.toolbar {
  margin-bottom: 16px;
}
</style>
