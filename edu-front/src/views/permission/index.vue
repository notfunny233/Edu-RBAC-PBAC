<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { pagePermissions, addPermission, updatePermission, deletePermissions } from '@/api/permission'
import { formatDateTime, nextDay } from '@/utils/format'
import { PERMISSION_TYPE_OPTIONS, labelOf } from '@/config/dict'

// ==================== 查询条件 ====================

const query = reactive({
  name: '',
  code: '',
  type: null,
  dateRange: []
})

// ==================== 分页列表 ====================

const loading = ref(false)
const permissionList = ref([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)
const selectedRows = ref([])

// 类型标签的颜色：菜单显眼一点、接口低调一点
const TYPE_TAG = {
  1: 'primary',
  2: 'warning',
  3: 'info'
}

async function loadPermissions() {
  loading.value = true
  try {
    const [begin, end] = query.dateRange || []
    const res = await pagePermissions({
      page: currentPage.value,
      pageSize: pageSize.value,
      name: query.name || undefined,
      code: query.code || undefined,
      type: query.type ?? undefined,
      // begin 和 end 必须同时传后端才会加日期条件；
      // 结束日期往后挪一天，否则选中的当天查不出来（见 utils/format.js 的 nextDay）
      begin: begin || undefined,
      end: begin ? nextDay(end) : undefined
    })
    permissionList.value = res.data.rows || []
    total.value = res.data.total || 0
  } finally {
    loading.value = false
  }
}

function indexMethod(index) {
  return (currentPage.value - 1) * pageSize.value + index + 1
}

function handleSearch() {
  currentPage.value = 1
  loadPermissions()
}

function handleReset() {
  query.name = ''
  query.code = ''
  query.type = null
  query.dateRange = []
  handleSearch()
}

function handlePageChange(page) {
  currentPage.value = page
  loadPermissions()
}

function handleSizeChange(size) {
  pageSize.value = size
  currentPage.value = 1
  loadPermissions()
}

function handleSelectionChange(rows) {
  selectedRows.value = rows
}

// ==================== 删除 ====================

// 通配权限点 * 后端不让删（删了超级管理员立刻失去全部权限）。
// 后端已经拦了一道，前端这里提前提示，免得用户点完才看到一个报错
function hasWildcard(rows) {
  return rows.some((row) => row.code === '*')
}

async function handleDelete(row) {
  if (row.code === '*') {
    ElMessage.warning('通配权限点 * 不允许删除，它是超级管理员的权限来源')
    return
  }
  try {
    await ElMessageBox.confirm(
      `确定要删除权限点「${row.name}」吗？删除后，所有引用了它的角色都会少掉这一项权限。`,
      '提示',
      { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' }
    )
  } catch {
    return
  }
  await deletePermissions([row.id])
  ElMessage.success('删除成功')
  loadPermissions()
}

async function handleBatchDelete() {
  if (selectedRows.value.length === 0) {
    ElMessage.warning('请先勾选要删除的权限点')
    return
  }
  if (hasWildcard(selectedRows.value)) {
    ElMessage.warning('选中的里面有通配权限点 *，它不允许删除，请先取消勾选')
    return
  }
  try {
    await ElMessageBox.confirm(
      `确定要删除选中的 ${selectedRows.value.length} 个权限点吗？删除后，所有引用它们的角色都会少掉这些权限。`,
      '提示',
      { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' }
    )
  } catch {
    return
  }
  await deletePermissions(selectedRows.value.map((row) => row.id))
  ElMessage.success('删除成功')
  selectedRows.value = []
  loadPermissions()
}

// ==================== 新增 / 编辑弹窗 ====================

const dialogVisible = ref(false)
const dialogTitle = ref('')
const submitting = ref(false)
const formRef = ref()

// 这几个字段的默认值不是随便写的：
// permission 表的 type / parent_id / sort 都是 NOT NULL，
// 而插入用的 SQL 是把所有列写死的（不是 <if> 动态拼的），
// 所以 **这三个字段一旦不传，就会直接撞数据库约束报错**，
// 不是那种能被 Service 拦下来给出友好提示的错误。
// 表单里给好默认值，用户不动也能正常提交
const form = reactive({
  id: null,
  name: '',
  code: '',
  type: 3,
  parentId: 0,
  sort: 0,
  remark: ''
})

// 编码格式校验。这条规则后端也会查一遍，这里提前拦是为了让用户当场看见问题。
// 为什么必须带冒号：编码是 resource:action 两段式（如 emp:delete），
// 决策引擎拿到接口注解后会按冒号拆出资源和操作去匹配。
// 写错了不会立刻报错，而是"授权成功了但接口永远校验不通过"，最难查的那种
function checkCode(rule, value, callback) {
  if (!value) {
    callback(new Error('请输入权限点编码'))
    return
  }
  if (value !== '*' && !value.includes(':')) {
    callback(new Error('要写成 resource:action 的格式，例如 emp:delete；只有通配权限点可以写成 *'))
    return
  }
  callback()
}

const rules = {
  name: [{ required: true, message: '请输入权限点名称', trigger: 'blur' }],
  code: [{ validator: checkCode, trigger: 'blur' }],
  type: [{ required: true, message: '请选择类型', trigger: 'change' }]
}

function resetForm() {
  form.id = null
  form.name = ''
  form.code = ''
  form.type = 3
  form.parentId = 0
  form.sort = 0
  form.remark = ''
}

function openAdd() {
  dialogTitle.value = '新增权限点'
  resetForm()
  dialogVisible.value = true
}

// 编辑直接用表格那一行填，不再请求详情接口（列表接口字段已经全了）
function openEdit(row) {
  dialogTitle.value = '编辑权限点'
  resetForm()
  form.id = row.id
  form.name = row.name
  form.code = row.code
  form.type = row.type
  form.parentId = row.parentId ?? 0
  form.sort = row.sort ?? 0
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
    // 六个字段全带上。type / parentId / sort 用的是 0 和 3 这种"假值"，
    // 不能过滤掉——它们不是"没填"，而是有意义的取值
    const payload = {
      id: form.id,
      name: form.name,
      code: form.code,
      type: form.type,
      parentId: form.parentId,
      sort: form.sort,
      remark: form.remark
    }

    if (form.id) {
      await updatePermission(payload)
      ElMessage.success('修改成功')
    } else {
      await addPermission(payload)
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    loadPermissions()
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  loadPermissions()
})
</script>

<template>
  <div class="permission-page">
    <!-- 查询条件 -->
    <el-card shadow="never" class="search-card">
      <el-form :model="query" inline>
        <el-form-item label="权限点名称">
          <el-input
            v-model="query.name"
            placeholder="如 删除员工"
            clearable
            class="w-180"
            @keyup.enter="handleSearch"
          />
        </el-form-item>

        <el-form-item label="权限点编码">
          <el-input
            v-model="query.code"
            placeholder="如 emp:delete"
            clearable
            class="w-180"
            @keyup.enter="handleSearch"
          />
        </el-form-item>

        <el-form-item label="类型">
          <el-select v-model="query.type" placeholder="全部" clearable class="w-120">
            <el-option
              v-for="item in PERMISSION_TYPE_OPTIONS"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
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
      <el-alert
        type="info"
        show-icon
        :closable="false"
        title="删除权限点要格外小心"
        description="接口上的注解写死的就是这些编码。把一个还在用的权限点删掉，对应接口对所有人都立刻变成 403，而且不会有人收到通知。"
        class="page-alert"
      />

      <div class="toolbar">
        <el-button type="primary" @click="openAdd">
          <el-icon><Plus /></el-icon>
          <span>新增权限点</span>
        </el-button>
        <el-button type="danger" :disabled="selectedRows.length === 0" @click="handleBatchDelete">
          批量删除
        </el-button>
      </div>

      <el-table
        v-loading="loading"
        :data="permissionList"
        border
        stripe
        row-key="id"
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="50" align="center" />
        <el-table-column type="index" label="序号" width="70" align="center" :index="indexMethod" />
        <el-table-column prop="name" label="权限点名称" min-width="130" />
        <el-table-column prop="code" label="权限点编码" min-width="150">
          <template #default="{ row }">
            <span :class="{ 'wildcard-code': row.code === '*' }">{{ row.code }}</span>
          </template>
        </el-table-column>
        <el-table-column label="类型" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="TYPE_TAG[row.type] || 'info'" size="small">
              {{ labelOf(PERMISSION_TYPE_OPTIONS, row.type) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="父级" width="90" align="center">
          <template #default="{ row }">
            {{ row.parentId === 0 ? '顶级' : row.parentId }}
          </template>
        </el-table-column>
        <el-table-column prop="sort" label="排序" width="80" align="center" />
        <el-table-column prop="remark" label="备注" min-width="200" show-overflow-tooltip>
          <template #default="{ row }">{{ row.remark || '-' }}</template>
        </el-table-column>
        <el-table-column label="创建时间" width="170" align="center">
          <template #default="{ row }">{{ formatDateTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="130" align="center" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="openEdit(row)">编辑</el-button>
            <el-button
              type="danger"
              link
              :disabled="row.code === '*'"
              @click="handleDelete(row)"
            >
              删除
            </el-button>
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
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="560px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="权限点名称" prop="name">
          <el-input v-model="form.name" placeholder="如 删除员工" maxlength="32" />
        </el-form-item>

        <el-form-item label="权限点编码" prop="code">
          <el-input v-model="form.code" placeholder="如 emp:delete" maxlength="64" />
          <div class="field-tip">
            写法和接口上的注解一一对应，改这里等于改"谁能调哪个接口"，尽量只增不改
          </div>
        </el-form-item>

        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="类型" prop="type">
              <el-select v-model="form.type" class="w-full">
                <el-option
                  v-for="item in PERMISSION_TYPE_OPTIONS"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="排序">
              <el-input-number v-model="form.sort" :min="0" :max="9999" class="w-full" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="父级ID">
          <el-input-number v-model="form.parentId" :min="0" :max="999999" class="w-full" />
          <div class="field-tip">0 表示顶级。当前库里所有权限点都是 0</div>
        </el-form-item>

        <el-form-item label="备注">
          <el-input
            v-model="form.remark"
            type="textarea"
            :rows="3"
            placeholder="这个权限点管什么"
            maxlength="200"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.permission-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.search-card :deep(.el-form-item) {
  margin-bottom: 0;
}

.w-120 {
  width: 120px;
}
.w-180 {
  width: 180px;
}
.w-260 {
  width: 260px;
}
.w-full {
  width: 100%;
}

.page-alert {
  margin-bottom: 16px;
}

.toolbar {
  margin-bottom: 16px;
}

.pager {
  margin-top: 16px;
  justify-content: flex-end;
}

.wildcard-code {
  font-weight: 700;
  color: var(--el-color-danger);
}

.field-tip {
  margin-top: 4px;
  color: var(--el-text-color-secondary);
  font-size: 12px;
  line-height: 1.5;
}
</style>
