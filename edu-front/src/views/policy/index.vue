<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { pagePolicies, addPolicy, updatePolicy, deletePolicies } from '@/api/policy'
import { listAllRoles } from '@/api/role'
import { listAllEmps } from '@/api/emp'
import { formatDateTime, nextDay } from '@/utils/format'
import {
  SUBJECT_TYPE_OPTIONS,
  EFFECT_OPTIONS,
  ENABLE_STATUS_OPTIONS,
  POLICY_RESOURCE_OPTIONS,
  POLICY_ACTION_OPTIONS,
  RESOURCE_LABELS,
  labelOf
} from '@/config/dict'

// ==================== 查询条件 ====================

const query = reactive({
  name: '',
  subjectType: null,
  resource: '',
  effect: null,
  status: null,
  dateRange: []
})

// ==================== 分页列表 ====================

const loading = ref(false)
const policyList = ref([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)
const selectedRows = ref([])

// 主体下拉的数据源。策略主体要么是角色、要么是员工，
// 但列表里只存了 subjectId 这个数字，要显示成名字就得自己对应一下
const roleOptions = ref([])
const empOptions = ref([])

const roleMap = computed(() => {
  const map = {}
  for (const role of roleOptions.value) {
    map[role.id] = role.name
  }
  return map
})

const empMap = computed(() => {
  const map = {}
  for (const emp of empOptions.value) {
    map[emp.id] = emp.name
  }
  return map
})

// 把 subjectId 翻译成"角色：班主任"这种能看懂的写法。
//
// 兜底那一路是有实际意义的：删角色时后端只清了 emp_role 和 role_permission
// 两张关联表，**没有清理 policy 里指向这个角色的策略**。
// 所以删完角色后，它的策略会留在列表里，subjectId 找不到对应的名字。
// 这类策略其实永远不会命中（没有员工再持有那个角色），属于无害的死数据，
// 但显示成 undefined 会让人以为页面坏了
function subjectLabel(row) {
  if (row.subjectType === 1) {
    const name = roleMap.value[row.subjectId]
    return name ? `角色：${name}` : `角色 #${row.subjectId}（已删除）`
  }
  if (row.subjectType === 2) {
    const name = empMap.value[row.subjectId]
    return name ? `员工：${name}` : `员工 #${row.subjectId}（已删除）`
  }
  return '-'
}

async function loadPolicies() {
  loading.value = true
  try {
    const [begin, end] = query.dateRange || []
    const res = await pagePolicies({
      page: currentPage.value,
      pageSize: pageSize.value,
      name: query.name || undefined,
      subjectType: query.subjectType ?? undefined,
      resource: query.resource || undefined,
      effect: query.effect ?? undefined,
      status: query.status ?? undefined,
      begin: begin || undefined,
      end: begin ? nextDay(end) : undefined
    })
    policyList.value = res.data.rows || []
    total.value = res.data.total || 0
  } finally {
    loading.value = false
  }
}

// 下拉和列表都依赖角色/员工名单，所以进页面就一起加载
async function loadOptions() {
  const [roleRes, empRes] = await Promise.all([listAllRoles(), listAllEmps()])
  roleOptions.value = roleRes.data || []
  empOptions.value = empRes.data || []
}

function indexMethod(index) {
  return (currentPage.value - 1) * pageSize.value + index + 1
}

function handleSearch() {
  currentPage.value = 1
  loadPolicies()
}

function handleReset() {
  query.name = ''
  query.subjectType = null
  query.resource = ''
  query.effect = null
  query.status = null
  query.dateRange = []
  handleSearch()
}

function handlePageChange(page) {
  currentPage.value = page
  loadPolicies()
}

function handleSizeChange(size) {
  pageSize.value = size
  currentPage.value = 1
  loadPolicies()
}

function handleSelectionChange(rows) {
  selectedRows.value = rows
}

// ==================== 删除 ====================

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(
      `确定要删除策略「${row.name}」吗？策略的作用是给权限加条件限制，删掉它等于把这条限制放开。`,
      '提示',
      { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' }
    )
  } catch {
    return
  }
  await deletePolicies([row.id])
  ElMessage.success('删除成功')
  loadPolicies()
}

async function handleBatchDelete() {
  if (selectedRows.value.length === 0) {
    ElMessage.warning('请先勾选要删除的策略')
    return
  }
  try {
    await ElMessageBox.confirm(
      `确定要删除选中的 ${selectedRows.value.length} 条策略吗？删掉它们等于把对应的限制放开。`,
      '提示',
      { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' }
    )
  } catch {
    return
  }
  await deletePolicies(selectedRows.value.map((row) => row.id))
  ElMessage.success('删除成功')
  selectedRows.value = []
  loadPolicies()
}

// ==================== 新增 / 编辑弹窗 ====================

const dialogVisible = ref(false)
const dialogTitle = ref('')
const submitting = ref(false)
const formRef = ref()

// 默认值说明：policy 表的 subject_type / subject_id / resource / action /
// effect / priority / status 都是 NOT NULL，而插入 SQL 把所有列都写死了
// （不是 <if> 动态拼的），所以任何一个不传都会直接撞数据库约束报错。
// 这里全部给上默认值
const form = reactive({
  id: null,
  name: '',
  subjectType: 1,
  subjectId: null,
  resource: 'emp',
  action: 'list',
  effect: 0,
  conditionExpr: '',
  priority: 0,
  status: 1,
  remark: ''
})

// 主体下拉跟着主体类型变：选角色就列角色，选员工就列员工
const subjectOptions = computed(() => {
  if (form.subjectType === 1) {
    return roleOptions.value.map((role) => ({ value: role.id, label: role.name }))
  }
  if (form.subjectType === 2) {
    return empOptions.value.map((emp) => ({ value: emp.id, label: emp.name }))
  }
  return []
})

// 切换主体类型时必须清掉已选的主体，
// 否则会留下"类型是角色、但 id 是某个员工"的错配，
// 这条策略存下去不会报错，但它永远不会命中——查都查不出来
function handleSubjectTypeChange() {
  form.subjectId = null
}

// 条件表达式的常用写法。给几个能直接用的例子，比让人对着空白框想要快得多。
// 变量就两个：#emp 是登录人实体，#params 是这次请求的参数（含路径变量）
const EXPR_SAMPLES = [
  { label: '仅限本部门', expr: '#params.deptId != null and #emp.deptId != #params.deptId' },
  { label: '排除某职位', expr: '#emp.job != 3' },
  { label: '不能操作自己', expr: '#emp.id == #params.id' }
]

function applyExpr(expr) {
  form.conditionExpr = expr
}

const rules = {
  name: [{ required: true, message: '请输入策略名称', trigger: 'blur' }],
  subjectType: [{ required: true, message: '请选择主体类型', trigger: 'change' }],
  subjectId: [{ required: true, message: '请选择主体', trigger: 'change' }],
  resource: [{ required: true, message: '请填写资源', trigger: 'blur' }],
  action: [{ required: true, message: '请填写操作', trigger: 'blur' }]
}

function resetForm() {
  form.id = null
  form.name = ''
  form.subjectType = 1
  form.subjectId = null
  form.resource = 'emp'
  form.action = 'list'
  // 效果默认给"拒绝"而不是"允许"：
  // 新建一条策略的动机通常是"想限制点什么"，默认拒绝更贴近意图，
  // 也避免手快点了确定就多出一条放行规则
  form.effect = 0
  form.conditionExpr = ''
  form.priority = 0
  form.status = 1
  form.remark = ''
}

function openAdd() {
  dialogTitle.value = '新增策略'
  resetForm()
  dialogVisible.value = true
}

function openEdit(row) {
  dialogTitle.value = '编辑策略'
  resetForm()
  form.id = row.id
  form.name = row.name
  form.subjectType = row.subjectType
  form.subjectId = row.subjectId
  form.resource = row.resource
  form.action = row.action
  form.effect = row.effect
  // 后端存的可能是 null，输入框要的是字符串，转一下
  form.conditionExpr = row.conditionExpr || ''
  form.priority = row.priority ?? 0
  form.status = row.status
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
      subjectType: form.subjectType,
      subjectId: form.subjectId,
      resource: form.resource,
      action: form.action,
      effect: form.effect,
      // 空字符串是合法的，表示"无条件生效"。
      // 不要传 null：后端的 SQL 判的是 != null，传 null 会被当成"这次不改这个字段"，
      // 结果就是想清空条件却清不掉
      conditionExpr: form.conditionExpr ?? '',
      priority: form.priority,
      status: form.status,
      remark: form.remark
    }

    if (form.id) {
      await updatePolicy(payload)
      ElMessage.success('修改成功')
    } else {
      await addPolicy(payload)
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    loadPolicies()
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  loadOptions()
  loadPolicies()
})
</script>

<template>
  <div class="policy-page">
    <!-- 查询条件 -->
    <el-card shadow="never" class="search-card">
      <el-form :model="query" inline>
        <el-form-item label="策略名称">
          <el-input
            v-model="query.name"
            placeholder="请输入策略名称"
            clearable
            class="w-180"
            @keyup.enter="handleSearch"
          />
        </el-form-item>

        <el-form-item label="主体类型">
          <el-select v-model="query.subjectType" placeholder="全部" clearable class="w-120">
            <el-option
              v-for="item in SUBJECT_TYPE_OPTIONS"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="资源">
          <el-select
            v-model="query.resource"
            placeholder="全部"
            clearable
            filterable
            allow-create
            class="w-140"
          >
            <el-option
              v-for="item in POLICY_RESOURCE_OPTIONS"
              :key="item"
              :label="RESOURCE_LABELS[item] || item"
              :value="item"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="效果">
          <el-select v-model="query.effect" placeholder="全部" clearable class="w-110">
            <el-option
              v-for="item in EFFECT_OPTIONS"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部" clearable class="w-110">
            <el-option
              v-for="item in ENABLE_STATUS_OPTIONS"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
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
        type="warning"
        show-icon
        :closable="false"
        title="策略比权限点更容易出错"
        description="权限点的答案是「有没有」，策略的答案是「在什么条件下才有」。一条写错的条件表达式会让一批人全部 403，而且报错只在服务端日志里。改之前先在测试账号上验一下。"
        class="page-alert"
      />

      <div class="toolbar">
        <el-button type="primary" @click="openAdd">
          <el-icon><Plus /></el-icon>
          <span>新增策略</span>
        </el-button>
        <el-button type="danger" :disabled="selectedRows.length === 0" @click="handleBatchDelete">
          批量删除
        </el-button>
      </div>

      <el-table
        v-loading="loading"
        :data="policyList"
        border
        stripe
        row-key="id"
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="50" align="center" />
        <el-table-column type="index" label="序号" width="70" align="center" :index="indexMethod" />
        <el-table-column prop="name" label="策略名称" min-width="180" show-overflow-tooltip />
        <el-table-column label="主体" width="160" show-overflow-tooltip>
          <template #default="{ row }">{{ subjectLabel(row) }}</template>
        </el-table-column>
        <el-table-column label="资源" width="110" align="center">
          <template #default="{ row }">
            {{ row.resource === '*' ? '不限' : RESOURCE_LABELS[row.resource] || row.resource }}
          </template>
        </el-table-column>
        <el-table-column prop="action" label="操作" width="100" align="center">
          <template #default="{ row }">{{ row.action === '*' ? '不限' : row.action }}</template>
        </el-table-column>
        <el-table-column label="效果" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.effect === 1 ? 'success' : 'danger'" size="small">
              {{ labelOf(EFFECT_OPTIONS, row.effect) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="条件表达式" min-width="240" show-overflow-tooltip>
          <template #default="{ row }">
            <span v-if="row.conditionExpr" class="expr-text">{{ row.conditionExpr }}</span>
            <span v-else class="expr-empty">无条件（对所有情况生效）</span>
          </template>
        </el-table-column>
        <el-table-column prop="priority" label="优先级" width="90" align="center" />
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
              {{ labelOf(ENABLE_STATUS_OPTIONS, row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="130" align="center" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="openEdit(row)">编辑</el-button>
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
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="680px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="策略名称" prop="name">
          <el-input v-model="form.name" placeholder="如 班主任不得删除部门" maxlength="64" />
        </el-form-item>

        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="主体类型" prop="subjectType">
              <el-select v-model="form.subjectType" class="w-full" @change="handleSubjectTypeChange">
                <el-option
                  v-for="item in SUBJECT_TYPE_OPTIONS"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item :label="form.subjectType === 1 ? '角色' : '员工'" prop="subjectId">
              <el-select
                v-model="form.subjectId"
                placeholder="请选择"
                filterable
                class="w-full"
              >
                <el-option
                  v-for="item in subjectOptions"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
                />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="资源" prop="resource">
              <el-select
                v-model="form.resource"
                filterable
                allow-create
                class="w-full"
                placeholder="如 emp"
              >
                <el-option
                  v-for="item in POLICY_RESOURCE_OPTIONS"
                  :key="item"
                  :label="item === '*' ? '* （不限资源）' : `${item}（${RESOURCE_LABELS[item] || '自定义'}）`"
                  :value="item"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="操作" prop="action">
              <el-select
                v-model="form.action"
                filterable
                allow-create
                class="w-full"
                placeholder="如 delete"
              >
                <el-option
                  v-for="item in POLICY_ACTION_OPTIONS"
                  :key="item"
                  :label="item === '*' ? '* （不限操作）' : item"
                  :value="item"
                />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="效果">
              <el-radio-group v-model="form.effect">
                <el-radio :value="0">拒绝</el-radio>
                <el-radio :value="1">允许</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="优先级">
              <el-input-number v-model="form.priority" :min="0" :max="9999" class="w-full" />
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="状态">
              <el-switch v-model="form.status" :active-value="1" :inactive-value="0" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="条件表达式">
          <el-input
            v-model="form.conditionExpr"
            type="textarea"
            :rows="3"
            placeholder="留空表示无条件生效。变量：#emp 是登录人，#params 是本次请求参数"
          />
          <div class="field-tip">
            <span>常用写法（点一下填入）：</span>
            <el-button
              v-for="sample in EXPR_SAMPLES"
              :key="sample.label"
              link
              type="primary"
              size="small"
              @click="applyExpr(sample.expr)"
            >
              {{ sample.label }}
            </el-button>
          </div>
          <div class="field-tip">
            优先级越大越先匹配，命中第一条就出结果。所以「拒绝」类策略的优先级要比「放行」类高，
            否则限制会被放行规则盖掉。
          </div>
        </el-form-item>

        <el-form-item label="备注">
          <el-input
            v-model="form.remark"
            type="textarea"
            :rows="2"
            placeholder="这条策略是为了解决什么问题"
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
.policy-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.search-card :deep(.el-form-item) {
  margin-bottom: 0;
}

.w-110 {
  width: 110px;
}
.w-120 {
  width: 120px;
}
.w-140 {
  width: 140px;
}
.w-180 {
  width: 180px;
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

.expr-text {
  font-family: Consolas, Monaco, monospace;
  font-size: 12px;
  color: var(--el-color-primary);
}

.expr-empty {
  color: var(--el-text-color-placeholder);
  font-size: 12px;
}

.field-tip {
  margin-top: 4px;
  color: var(--el-text-color-secondary);
  font-size: 12px;
  line-height: 1.8;
}
</style>
