<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { pageEmps, getEmp, addEmp, updateEmp, deleteEmps } from '@/api/emp'
import { listDepts } from '@/api/dept'
import { uploadFile } from '@/api/upload'
import { GENDER_OPTIONS, JOB_OPTIONS, labelOf } from '@/config/dict'
import { formatDate } from '@/utils/format'

// ==================== 查询条件 ====================

const query = reactive({
  name: '',
  gender: null,
  // 日期范围先用一个数组接住，提交时再拆成 begin 和 end 两个参数。
  // 用 daterange 这种控件的好处是：它要么给两个日期，要么一个都不给，
  // 天然满足后端"begin 和 end 必须同时传"的要求。
  dateRange: []
})

// ==================== 分页列表 ====================

const loading = ref(false)
const empList = ref([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)

async function loadEmps() {
  loading.value = true
  try {
    const [begin, end] = query.dateRange || []
    const res = await pageEmps({
      page: currentPage.value,
      pageSize: pageSize.value,
      // 空字符串和 null 都换成 undefined，axios 会自动跳过 undefined 的参数，
      // 这样后端收到的就是"没传这个条件"，而不是"传了个空字符串"
      name: query.name || undefined,
      gender: query.gender ?? undefined,
      begin: begin || undefined,
      end: end || undefined
    })
    empList.value = res.data.rows || []
    total.value = res.data.total || 0
  } finally {
    loading.value = false
  }
}

// 序号要跨页连续，不能每页都从 1 开始
function indexMethod(index) {
  return (currentPage.value - 1) * pageSize.value + index + 1
}

function handleSearch() {
  // 条件变了必须回到第一页。否则你在第 5 页筛数据，筛选后只剩 1 页，
  // 就会看到一张空表格，还以为是"没查到"
  currentPage.value = 1
  loadEmps()
}

function handleReset() {
  query.name = ''
  query.gender = null
  query.dateRange = []
  handleSearch()
}

function handlePageChange(page) {
  currentPage.value = page
  loadEmps()
}

function handleSizeChange(size) {
  pageSize.value = size
  currentPage.value = 1
  loadEmps()
}

// ==================== 删除 ====================
//
// 后端只有一个"批量删除"接口，但传一个 id 的数组就是删一条，
// 所以单个删除和批量删除走同一个接口，不用写两套。

const selection = ref([])

function handleSelectionChange(rows) {
  selection.value = rows
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(`确定要删除「${row.name}」吗？`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
  } catch {
    return
  }
  await deleteEmps([row.id])
  ElMessage.success('删除成功')
  loadEmps()
}

async function handleBatchDelete() {
  if (!selection.value.length) {
    ElMessage.warning('请先勾选要删除的员工')
    return
  }
  try {
    await ElMessageBox.confirm(
      `确定要删除选中的 ${selection.value.length} 名员工吗？`,
      '提示',
      { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' }
    )
  } catch {
    return
  }
  await deleteEmps(selection.value.map((row) => row.id))
  ElMessage.success('删除成功')
  loadEmps()
}

// ==================== 部门下拉 ====================

const deptList = ref([])

async function loadDepts() {
  const res = await listDepts()
  deptList.value = res.data || []
}

// ==================== 新增 / 编辑弹窗 ====================

const dialogVisible = ref(false)
const dialogTitle = ref('')
const submitting = ref(false)
const formRef = ref()

const form = reactive({
  id: null,
  username: '',
  name: '',
  gender: null,
  phone: '',
  job: null,
  salary: null,
  entryDate: '',
  deptId: null,
  image: '',
  exprList: []
})

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  name: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  gender: [{ required: true, message: '请选择性别', trigger: 'change' }],
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1\d{10}$/, message: '手机号应该是 1 开头的 11 位数字', trigger: 'blur' }
  ],
  job: [{ required: true, message: '请选择职位', trigger: 'change' }],
  entryDate: [{ required: true, message: '请选择入职日期', trigger: 'change' }]
}

function resetForm() {
  form.id = null
  form.username = ''
  form.name = ''
  form.gender = null
  form.phone = ''
  form.job = null
  form.salary = null
  form.entryDate = ''
  form.deptId = null
  form.image = ''
  form.exprList = []
}

function openAdd() {
  dialogTitle.value = '新增员工'
  resetForm()
  dialogVisible.value = true
}

async function openEdit(row) {
  dialogTitle.value = '编辑员工'
  resetForm()

  // 这里必须重新请求一次详情，不能像部门那样直接拿表格行数据填。
  // 因为表格里没有工作经历，只有详情接口会把 exprList 一起返回。
  const res = await getEmp(row.id)
  const emp = res.data || {}

  form.id = emp.id
  form.username = emp.username
  form.name = emp.name
  form.gender = emp.gender
  form.phone = emp.phone
  form.job = emp.job
  form.salary = emp.salary
  form.entryDate = formatDate(emp.entryDate) === '-' ? '' : formatDate(emp.entryDate)
  form.deptId = emp.deptId
  form.image = emp.image

  // 后端查详情用的是 left join，员工没有工作经历时也会返回**一条字段全是 null** 的记录。
  // 直接塞进表单会变成一行空输入框，看着像坏了，所以这里把这种空记录过滤掉。
  form.exprList = (emp.exprList || []).filter(
    (item) => item && (item.company || item.job || item.begin || item.end)
  )

  dialogVisible.value = true
}

function addExpr() {
  form.exprList.push({ begin: '', end: '', company: '', job: '' })
}

function removeExpr(index) {
  form.exprList.splice(index, 1)
}

async function handleSubmit() {
  try {
    await formRef.value.validate()
  } catch {
    return
  }

  submitting.value = true
  try {
    const payload = { ...form }
    if (form.id) {
      await updateEmp(payload)
      ElMessage.success('修改成功')
    } else {
      await addEmp(payload)
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    loadEmps()
  } finally {
    submitting.value = false
  }
}

// ==================== 头像上传 ====================

const uploading = ref(false)

function beforeUpload(file) {
  if (!file.type.startsWith('image/')) {
    ElMessage.error('只能上传图片')
    return false
  }
  if (file.size / 1024 / 1024 >= 5) {
    ElMessage.error('图片不能超过 5MB')
    return false
  }
  return true
}

// 用自定义上传，而不是 el-upload 自带的 action。
// 这样请求会走我们自己的 axios 封装，token 和错误提示都不用再配一遍。
async function handleUpload(options) {
  uploading.value = true
  try {
    const res = await uploadFile(options.file)
    form.image = res.data
    ElMessage.success('上传成功')
  } finally {
    uploading.value = false
  }
}

// ==================== 初始化 ====================

onMounted(() => {
  loadDepts()
  loadEmps()
})
</script>

<template>
  <div class="emp-page">
    <!-- 查询条件 -->
    <el-card shadow="never" class="search-card">
      <el-form :model="query" inline>
        <el-form-item label="姓名">
          <el-input
            v-model="query.name"
            placeholder="请输入姓名"
            clearable
            class="w-160"
            @keyup.enter="handleSearch"
          />
        </el-form-item>

        <el-form-item label="性别">
          <el-select v-model="query.gender" placeholder="全部" clearable class="w-110">
            <el-option
              v-for="item in GENDER_OPTIONS"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="入职日期">
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
          <span>新增员工</span>
        </el-button>
        <el-button type="danger" :disabled="!selection.length" @click="handleBatchDelete">
          批量删除
        </el-button>
      </div>

      <el-table
        v-loading="loading"
        :data="empList"
        border
        stripe
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="50" align="center" />
        <el-table-column type="index" label="序号" width="70" align="center" :index="indexMethod" />
        <el-table-column prop="username" label="用户名" width="120" />
        <el-table-column prop="name" label="姓名" width="100" />
        <el-table-column label="性别" width="70" align="center">
          <template #default="{ row }">{{ labelOf(GENDER_OPTIONS, row.gender) }}</template>
        </el-table-column>
        <el-table-column prop="phone" label="手机号" width="130" />
        <el-table-column label="职位" width="100">
          <template #default="{ row }">{{ labelOf(JOB_OPTIONS, row.job) }}</template>
        </el-table-column>
        <el-table-column prop="salary" label="薪资" width="100" />
        <el-table-column label="入职日期" width="120">
          <template #default="{ row }">{{ formatDate(row.entryDate) }}</template>
        </el-table-column>
        <el-table-column prop="deptName" label="所属部门" min-width="120" />
        <el-table-column label="操作" width="140" align="center" fixed="right">
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
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="760px" top="5vh">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="用户名" prop="username">
              <el-input v-model="form.username" placeholder="登录用的账号" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="姓名" prop="name">
              <el-input v-model="form.name" placeholder="请输入姓名" />
            </el-form-item>
          </el-col>

          <el-col :span="12">
            <el-form-item label="性别" prop="gender">
              <el-select v-model="form.gender" placeholder="请选择" class="w-full">
                <el-option
                  v-for="item in GENDER_OPTIONS"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="手机号" prop="phone">
              <el-input v-model="form.phone" placeholder="请输入手机号" maxlength="11" />
            </el-form-item>
          </el-col>

          <el-col :span="12">
            <el-form-item label="职位" prop="job">
              <el-select v-model="form.job" placeholder="请选择" class="w-full">
                <el-option
                  v-for="item in JOB_OPTIONS"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="薪资">
              <el-input-number v-model="form.salary" :min="0" :max="100000" class="w-full" />
            </el-form-item>
          </el-col>

          <el-col :span="12">
            <el-form-item label="入职日期" prop="entryDate">
              <el-date-picker
                v-model="form.entryDate"
                type="date"
                placeholder="请选择日期"
                value-format="YYYY-MM-DD"
                class="w-full"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="所属部门">
              <el-select v-model="form.deptId" placeholder="请选择" clearable class="w-full">
                <el-option
                  v-for="item in deptList"
                  :key="item.id"
                  :label="item.name"
                  :value="item.id"
                />
              </el-select>
            </el-form-item>
          </el-col>

          <el-col :span="24">
            <el-form-item label="头像">
              <div v-loading="uploading">
                <el-upload
                  :show-file-list="false"
                  :http-request="handleUpload"
                  :before-upload="beforeUpload"
                >
                  <img v-if="form.image" :src="form.image" class="avatar" alt="头像" />
                  <div v-else class="avatar avatar-empty">
                    <el-icon><Plus /></el-icon>
                  </div>
                </el-upload>
                <div class="upload-tip">不传也行，支持图片，不超过 5MB</div>
              </div>
            </el-form-item>
          </el-col>

          <el-col :span="24">
            <el-form-item label="工作经历">
              <div class="expr-box">
                <div v-for="(expr, index) in form.exprList" :key="index" class="expr-row">
                  <el-date-picker
                    v-model="expr.begin"
                    type="date"
                    placeholder="开始时间"
                    value-format="YYYY-MM-DD"
                    class="expr-date"
                  />
                  <el-date-picker
                    v-model="expr.end"
                    type="date"
                    placeholder="结束时间"
                    value-format="YYYY-MM-DD"
                    class="expr-date"
                  />
                  <el-input v-model="expr.company" placeholder="公司名称" class="expr-input" />
                  <el-input v-model="expr.job" placeholder="职位" class="expr-input" />
                  <el-button type="danger" link @click="removeExpr(index)">删除</el-button>
                </div>

                <el-button type="primary" link @click="addExpr">
                  <el-icon><Plus /></el-icon>
                  <span>添加工作经历</span>
                </el-button>
                <div v-if="!form.exprList.length" class="expr-empty">
                  没有工作经历可以不填
                </div>
              </div>
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.emp-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.search-card :deep(.el-form-item) {
  margin-bottom: 0;
}

.w-160 {
  width: 160px;
}
.w-110 {
  width: 110px;
}
.w-260 {
  width: 260px;
}
.w-full {
  width: 100%;
}

.toolbar {
  margin-bottom: 16px;
}

.pager {
  margin-top: 16px;
  justify-content: flex-end;
}

/* 头像上传 */
.avatar {
  width: 90px;
  height: 90px;
  border-radius: 6px;
  object-fit: cover;
  display: block;
}

.avatar-empty {
  display: flex;
  align-items: center;
  justify-content: center;
  border: 1px dashed #d9d9d9;
  color: #8c939d;
  font-size: 22px;
  cursor: pointer;
}

.avatar-empty:hover {
  border-color: #409eff;
  color: #409eff;
}

.upload-tip {
  margin-top: 6px;
  font-size: 12px;
  color: #909399;
}

/* 工作经历 */
.expr-box {
  width: 100%;
}

.expr-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.expr-date {
  width: 150px;
  flex: none;
}

.expr-input {
  flex: 1;
}

.expr-empty {
  margin-top: 4px;
  font-size: 12px;
  color: #909399;
}
</style>
