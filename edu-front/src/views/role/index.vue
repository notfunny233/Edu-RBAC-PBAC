<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { pageRoles, addRole, updateRole, deleteRoles } from '@/api/role'
import { listAllPermissions } from '@/api/permission'
import { getRolePermissions, assignRolePermissions } from '@/api/auth'
import { formatDateTime, nextDay } from '@/utils/format'
import { RESOURCE_LABELS } from '@/config/dict'

// ==================== 查询条件 ====================

const query = reactive({
  name: '',
  code: '',
  // 创建时间范围先用数组接住，查询时再拆成 begin 和 end
  dateRange: []
})

// ==================== 分页列表 ====================

const loading = ref(false)
const roleList = ref([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)
const selectedRows = ref([])

async function loadRoles() {
  loading.value = true
  try {
    const [begin, end] = query.dateRange || []
    const res = await pageRoles({
      page: currentPage.value,
      pageSize: pageSize.value,
      name: query.name || undefined,
      code: query.code || undefined,
      // begin 和 end 必须同时传，后端才会加日期条件（XML 里是两个一起判空的）。
      // daterange 控件要么给两个日期、要么一个都不给，所以判断 begin 就够了。
      // 结束日期往后挪一天，否则"选到 9 月 18 日"查不到 9 月 18 日当天——
      // 原因见 utils/format.js 里 nextDay 的注释
      begin: begin || undefined,
      end: begin ? nextDay(end) : undefined
    })
    roleList.value = res.data.rows || []
    total.value = res.data.total || 0
  } finally {
    loading.value = false
  }
}

// 序号跨页连续
function indexMethod(index) {
  return (currentPage.value - 1) * pageSize.value + index + 1
}

function handleSearch() {
  // 条件变了要回第一页，否则在第 3 页筛选、筛完只剩 1 页，会看到空表格
  currentPage.value = 1
  loadRoles()
}

function handleReset() {
  query.name = ''
  query.code = ''
  query.dateRange = []
  handleSearch()
}

function handlePageChange(page) {
  currentPage.value = page
  loadRoles()
}

function handleSizeChange(size) {
  pageSize.value = size
  currentPage.value = 1
  loadRoles()
}

function handleSelectionChange(rows) {
  selectedRows.value = rows
}

// ==================== 删除 ====================

// 单个删除也走批量接口（传一个 id 的数组），后端只有一个删除接口
async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(
      `确定要删除角色「${row.name}」吗？删除后，原本用这个角色的员工会立刻失去对应权限。`,
      '提示',
      { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' }
    )
  } catch {
    return
  }
  await deleteRoles([row.id])
  ElMessage.success('删除成功')
  loadRoles()
}

async function handleBatchDelete() {
  if (selectedRows.value.length === 0) {
    ElMessage.warning('请先勾选要删除的角色')
    return
  }
  try {
    await ElMessageBox.confirm(
      `确定要删除选中的 ${selectedRows.value.length} 个角色吗？删除后，原本用这些角色的员工会立刻失去对应权限。`,
      '提示',
      { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' }
    )
  } catch {
    return
  }
  await deleteRoles(selectedRows.value.map((row) => row.id))
  ElMessage.success('删除成功')
  selectedRows.value = []
  loadRoles()
}

// ==================== 新增 / 编辑弹窗 ====================

const dialogVisible = ref(false)
const dialogTitle = ref('')
const submitting = ref(false)
const formRef = ref()

const form = reactive({
  id: null,
  name: '',
  code: '',
  remark: ''
})

// code 是唯一键，后端会查重并给出能看懂的提示（"角色编码 xxx 已存在"），
// 所以前端不重复做唯一性校验，只保证必填和格式
const rules = {
  name: [{ required: true, message: '请输入角色名称', trigger: 'blur' }],
  code: [
    { required: true, message: '请输入角色编码', trigger: 'blur' },
    {
      pattern: /^[a-z][a-z0-9_]*$/,
      message: '只能用英文小写字母、数字和下划线，并且以字母开头，例如 head_teacher',
      trigger: 'blur'
    }
  ]
}

function resetForm() {
  form.id = null
  form.name = ''
  form.code = ''
  form.remark = ''
}

function openAdd() {
  dialogTitle.value = '新增角色'
  resetForm()
  dialogVisible.value = true
}

// 编辑直接用表格那一行数据填表单，不再请求详情接口——
// 列表接口已经把 name / code / remark 全返回了，多请求一次纯属浪费
function openEdit(row) {
  dialogTitle.value = '编辑角色'
  resetForm()
  form.id = row.id
  form.name = row.name
  form.code = row.code
  form.remark = row.remark
  dialogVisible.value = true
}

async function handleSubmit() {
  try {
    await formRef.value.validate()
  } catch {
    return
  }

  submitting.value = true
  try {
    const payload = {
      id: form.id,
      name: form.name,
      code: form.code,
      remark: form.remark
    }

    if (form.id) {
      await updateRole(payload)
      ElMessage.success('修改成功')
    } else {
      await addRole(payload)
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    loadRoles()
  } finally {
    submitting.value = false
  }
}

// ==================== 给角色授权（配权限点）====================
//
// 对接后端的 /auth/permissions/{roleId}：
// 进来先 GET 拿到"这个角色现在有哪些权限点"，点保存再 PUT 把勾选结果整体覆盖过去。
//
// ⚠️ 这里传的是详情，和"编辑角色"不一样，必须重新请求：
// 角色列表里根本没有权限点信息，只能去授权接口拿。

const authVisible = ref(false)
const authLoading = ref(false)
const authSaving = ref(false)
const authRoleId = ref(null)
const authRoleName = ref('')
const allPermissions = ref([])
// 勾选的权限点 id，el-checkbox-group 直接绑它
const checkedIds = ref([])

// 把权限点按编码里的"资源"段分组（emp:list / emp:delete 都归到"员工"）。
//
// 为什么不按 parent_id 拼树：库里 32 条权限点的 parent_id 全是 0，
// 拼出来是一棵所有节点都挂在根上的假树，还不如按资源分组看得清楚。
//
// 分组顺序跟着后端返回的 sort 走，通配权限点 * 的 sort 是 0，自然排在最前面
const permissionGroups = computed(() => {
  const groups = []
  const groupMap = new Map()

  for (const item of allPermissions.value) {
    // 编码形如 resource:action；通配权限点是一根光秃秃的 *，没有冒号，单独归一组
    const resource = item.code && item.code.includes(':') ? item.code.split(':')[0] : '*'
    let group = groupMap.get(resource)
    if (!group) {
      group = {
        resource,
        label: resource === '*' ? '全部权限' : RESOURCE_LABELS[resource] || resource,
        items: []
      }
      groupMap.set(resource, group)
      groups.push(group)
    }
    group.items.push(item)
  }

  // 全部权限排最前，其余按组内第一条的 sort 排
  groups.sort((a, b) => {
    if (a.resource === '*') return -1
    if (b.resource === '*') return 1
    return (a.items[0]?.sort ?? 0) - (b.items[0]?.sort ?? 0)
  })

  return groups
})

// 勾选通配权限点 * 时给个显眼提示：这一条等于全部权限
const wildcardChecked = computed(() => {
  return allPermissions.value.some((item) => item.code === '*' && checkedIds.value.includes(item.id))
})

function isGroupAllChecked(group) {
  return group.items.length > 0 && group.items.every((item) => checkedIds.value.includes(item.id))
}

// 勾了一部分时，组标题的勾选框要显示成"半选"（一道横杠）
function isGroupIndeterminate(group) {
  const count = countCheckedInGroup(group)
  return count > 0 && count < group.items.length
}

function countCheckedInGroup(group) {
  return group.items.filter((item) => checkedIds.value.includes(item.id)).length
}

// 单个权限点的勾选。
//
// 这里没有用 el-checkbox-group 包一层，而是每个勾选框自己算状态、自己改数组。
// 原因：分组渲染时会有十几个 el-checkbox-group 同时绑同一个数组，
// 属于框架的边缘用法，出问题时不容易看出是哪一层的问题。
// 自己管反而一目了然：勾上就加、取消就去掉
function toggleItem(id, checked) {
  if (checked) {
    // 用 Set 去重，防止重复点出现重复 id
    checkedIds.value = Array.from(new Set([...checkedIds.value, id]))
  } else {
    checkedIds.value = checkedIds.value.filter((item) => item !== id)
  }
}

// 点组标题 = 整组全勾 / 整组取消
function toggleGroup(group, checked) {
  const groupIds = group.items.map((item) => item.id)
  if (checked) {
    checkedIds.value = Array.from(new Set([...checkedIds.value, ...groupIds]))
  } else {
    checkedIds.value = checkedIds.value.filter((id) => !groupIds.includes(id))
  }
}

async function openAuth(row) {
  authRoleId.value = row.id
  authRoleName.value = row.name
  authVisible.value = true
  authLoading.value = true
  try {
    // 两个请求互不依赖，一起发，省一个来回
    const [permRes, rolePermRes] = await Promise.all([
      listAllPermissions(),
      getRolePermissions(row.id)
    ])
    allPermissions.value = permRes.data || []
    checkedIds.value = rolePermRes.data || []
  } finally {
    authLoading.value = false
  }
}

async function handleAuthSubmit() {
  authSaving.value = true
  try {
    // 覆盖式提交：这份数组就是"保存后应该有的完整集合"。
    // 全部取消勾选时传的是空数组 []，这是合法的，后端会把该角色的权限清空
    await assignRolePermissions(authRoleId.value, checkedIds.value)
    ElMessage.success('授权成功')
    authVisible.value = false
  } finally {
    authSaving.value = false
  }
}

onMounted(() => {
  loadRoles()
})
</script>

<template>
  <div class="role-page">
    <!-- 查询条件 -->
    <el-card shadow="never" class="search-card">
      <el-form :model="query" inline>
        <el-form-item label="角色名称">
          <el-input
            v-model="query.name"
            placeholder="请输入角色名称"
            clearable
            class="w-180"
            @keyup.enter="handleSearch"
          />
        </el-form-item>

        <el-form-item label="角色编码">
          <el-input
            v-model="query.code"
            placeholder="如 head_teacher"
            clearable
            class="w-180"
            @keyup.enter="handleSearch"
          />
        </el-form-item>

        <el-form-item label="创建时间">
          <el-date-picker
            v-model="query.dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD"
            class="w-260"
          />
        </el-form-item>

        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 列表 -->
    <el-card shadow="never">
      <div class="toolbar">
        <el-button type="primary" @click="openAdd">
          <el-icon><Plus /></el-icon>
          <span>新增角色</span>
        </el-button>
        <el-button type="danger" :disabled="selectedRows.length === 0" @click="handleBatchDelete">
          批量删除
        </el-button>
      </div>

      <el-table
        v-loading="loading"
        :data="roleList"
        border
        stripe
        row-key="id"
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="50" align="center" />
        <el-table-column type="index" label="序号" width="70" align="center" :index="indexMethod" />
        <el-table-column prop="name" label="角色名称" min-width="140" />
        <el-table-column prop="code" label="角色编码" min-width="160" />
        <el-table-column prop="remark" label="备注" min-width="220" show-overflow-tooltip>
          <template #default="{ row }">{{ row.remark || '-' }}</template>
        </el-table-column>
        <el-table-column label="创建时间" width="170" align="center">
          <template #default="{ row }">{{ formatDateTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="180" align="center" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="openEdit(row)">编辑</el-button>
            <el-button type="primary" link @click="openAuth(row)">授权</el-button>
            <el-button type="danger" link @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        class="pager"
        :current-page="currentPage"
        :page-size="pageSize"
        :total="total"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        @current-change="handlePageChange"
        @size-change="handleSizeChange"
      />
    </el-card>

    <!-- 新增 / 编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="520px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="角色名称" prop="name">
          <el-input v-model="form.name" placeholder="如 班主任" maxlength="30" />
        </el-form-item>

        <el-form-item label="角色编码" prop="code">
          <el-input v-model="form.code" placeholder="如 head_teacher" maxlength="50" />
        </el-form-item>

        <el-form-item label="备注">
          <el-input
            v-model="form.remark"
            type="textarea"
            :rows="3"
            placeholder="这个角色负责什么"
            maxlength="200"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- 授权抽屉：给这个角色勾权限点 -->
    <el-drawer v-model="authVisible" size="620px" :destroy-on-close="true">
      <template #header>
        <div class="auth-header">
          给「{{ authRoleName }}」授权
        </div>
      </template>

      <div v-loading="authLoading" class="auth-body">
        <el-alert
          v-if="wildcardChecked"
          type="warning"
          show-icon
          :closable="false"
          title="已勾选通配权限点 *"
          description="这一条等于全部权限。保存后该角色会拥有所有接口的访问权，上面勾的其他权限点都变成多余的。"
          class="auth-alert"
        />

        <div class="auth-tip">
          勾选后点「保存」整体覆盖。已经拥有的权限会在打开时自动勾上。
        </div>

        <div v-for="group in permissionGroups" :key="group.resource" class="perm-group">
          <div class="perm-group-head">
            <el-checkbox
              :model-value="isGroupAllChecked(group)"
              :indeterminate="isGroupIndeterminate(group)"
              @change="(checked) => toggleGroup(group, checked)"
            >
              <span class="group-label">{{ group.label }}</span>
            </el-checkbox>
            <span class="group-count">{{ countCheckedInGroup(group) }} / {{ group.items.length }}</span>
          </div>

          <div class="perm-items">
            <el-checkbox
              v-for="item in group.items"
              :key="item.id"
              :model-value="checkedIds.includes(item.id)"
              @change="(checked) => toggleItem(item.id, checked)"
            >
              {{ item.name }}
              <span class="perm-code">{{ item.code }}</span>
            </el-checkbox>
          </div>
        </div>

        <el-empty v-if="!authLoading && permissionGroups.length === 0" description="没有查到权限点" />
      </div>

      <template #footer>
        <el-button @click="clearAllChecked">全部取消</el-button>
        <el-button @click="authVisible = false">取消</el-button>
        <el-button type="primary" :loading="authSaving" @click="handleAuthSubmit">保存</el-button>
      </template>
    </el-drawer>
  </div>
</template>

<style scoped>
.role-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.search-card :deep(.el-form-item) {
  margin-bottom: 0;
}

.w-180 {
  width: 180px;
}
.w-260 {
  width: 260px;
}

.toolbar {
  margin-bottom: 16px;
}

.pager {
  margin-top: 16px;
  justify-content: flex-end;
}

/* ---------- 授权抽屉 ---------- */

.auth-header {
  font-size: 16px;
  font-weight: 600;
}

.auth-body {
  min-height: 200px;
}

.auth-alert {
  margin-bottom: 12px;
}

.auth-tip {
  margin-bottom: 16px;
  color: var(--el-text-color-secondary);
  font-size: 13px;
}

.perm-group {
  margin-bottom: 8px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 4px;
}

.perm-group-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 12px;
  background: var(--el-fill-color-light);
  border-radius: 4px 4px 0 0;
}

.group-label {
  font-weight: 600;
}

.group-count {
  color: var(--el-text-color-secondary);
  font-size: 12px;
}

.perm-items {
  display: flex;
  flex-wrap: wrap;
  gap: 4px 8px;
  padding: 12px;
}

/* 勾选框排成两列，32 个权限点铺开也不至于太长 */
.perm-items :deep(.el-checkbox) {
  width: calc(50% - 8px);
  margin-right: 0;
}

.perm-code {
  margin-left: 4px;
  color: var(--el-text-color-placeholder);
  font-size: 12px;
}
</style>
