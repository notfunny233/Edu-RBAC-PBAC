<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  pageStudents,
  addStudent,
  updateStudent,
  deleteStudents,
  deductScore
} from '@/api/student'
import { listAllClazzs } from '@/api/clazz'
import { GENDER_OPTIONS, DEGREE_OPTIONS, IS_COLLEGE_OPTIONS, labelOf } from '@/config/dict'
import { formatDate } from '@/utils/format'

// ==================== 查询条件 ====================
//
// 后端只支持姓名、学历、班级三个筛选条件，没有性别、也没有日期范围。
// 所以这里就做三个控件，多做了后端也不认

const query = reactive({
  name: '',
  degree: null,
  clazzId: null
})

// ==================== 分页列表 ====================

const loading = ref(false)
const studentList = ref([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)

async function loadStudents() {
  loading.value = true
  try {
    const res = await pageStudents({
      page: currentPage.value,
      pageSize: pageSize.value,
      // 空字符串和 null 都换成 undefined，axios 会自动跳过 undefined 的参数，
      // 这样后端收到的是"没传这个条件"，而不是"传了个空字符串"
      name: query.name || undefined,
      degree: query.degree ?? undefined,
      clazzId: query.clazzId ?? undefined
    })
    studentList.value = res.data.rows || []
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
  // 条件变了必须回到第一页，否则在第 2 页筛数据、筛完只剩 1 页，
  // 就会看到一张空表格，还以为是"没查到"
  currentPage.value = 1
  loadStudents()
}

function handleReset() {
  query.name = ''
  query.degree = null
  query.clazzId = null
  handleSearch()
}

function handlePageChange(page) {
  currentPage.value = page
  loadStudents()
}

function handleSizeChange(size) {
  pageSize.value = size
  currentPage.value = 1
  loadStudents()
}

// ==================== 班级下拉 ====================

const clazzList = ref([])

async function loadClazzs() {
  const res = await listAllClazzs()
  clazzList.value = res.data || []
}

// ==================== 删除 ====================
//
// 这个模块的删除接口和员工、部门都不一样：id 拼在路径里、用逗号分隔。
// 单个删除也是走这个接口，传一个 id 就行，所以两处共用一个函数

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
  await deleteStudents([row.id])
  ElMessage.success('删除成功')
  loadStudents()
}

async function handleBatchDelete() {
  if (!selection.value.length) {
    ElMessage.warning('请先勾选要删除的学生')
    return
  }
  try {
    await ElMessageBox.confirm(
      `确定要删除选中的 ${selection.value.length} 名学生吗？`,
      '提示',
      { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' }
    )
  } catch {
    return
  }
  await deleteStudents(selection.value.map((row) => row.id))
  ElMessage.success('删除成功')
  loadStudents()
}

// ==================== 违纪扣分 ====================
//
// 单独一个接口，不是改学生信息。
// 后端的行为是：分数够就减、不够直接归零，同时违纪次数 +1

const violationVisible = ref(false)
const violationSubmitting = ref(false)
const violationForm = reactive({ id: null, name: '', score: null })

function openViolation(row) {
  violationForm.id = row.id
  violationForm.name = row.name
  violationForm.score = null
  violationVisible.value = true
}

async function handleViolation() {
  if (!violationForm.score || violationForm.score <= 0) {
    ElMessage.warning('请输入大于 0 的扣分数')
    return
  }

  violationSubmitting.value = true
  try {
    await deductScore(violationForm.id, violationForm.score)
    ElMessage.success('扣分成功')
    violationVisible.value = false
    loadStudents()
  } finally {
    violationSubmitting.value = false
  }
}

// ==================== 新增 / 编辑弹窗 ====================

const dialogVisible = ref(false)
const dialogTitle = ref('')
const submitting = ref(false)
const formRef = ref()

const form = reactive({
  id: null,
  name: '',
  no: '',
  gender: null,
  phone: '',
  degree: null,
  idCard: '',
  isCollege: null,
  address: '',
  graduationDate: '',
  clazzId: null
})

// 必填项是照着数据库的 NOT NULL 抄的：姓名、学号、性别、手机号、身份证、是否院校、班级。
// 学历、联系地址、毕业时间在数据库里允许为空，所以这里也不强制
const rules = {
  name: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  no: [{ required: true, message: '请输入学号', trigger: 'blur' }],
  gender: [{ required: true, message: '请选择性别', trigger: 'change' }],
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1\d{10}$/, message: '手机号应该是 1 开头的 11 位数字', trigger: 'blur' }
  ],
  idCard: [
    { required: true, message: '请输入身份证号', trigger: 'blur' },
    { pattern: /^\d{17}[\dXx]$/, message: '身份证号应该是 18 位', trigger: 'blur' }
  ],
  isCollege: [{ required: true, message: '请选择是否来自院校', trigger: 'change' }],
  clazzId: [{ required: true, message: '请选择班级', trigger: 'change' }]
}

function resetForm() {
  form.id = null
  form.name = ''
  form.no = ''
  form.gender = null
  form.phone = ''
  form.degree = null
  form.idCard = ''
  form.isCollege = null
  form.address = ''
  form.graduationDate = ''
  form.clazzId = null
}

function openAdd() {
  dialogTitle.value = '新增学生'
  resetForm()
  dialogVisible.value = true
}

// 编辑直接用表格里那一行数据填，不再请求详情接口。
// 列表查询用的是 select s.*，编辑要用的字段一个不少，多请求一次纯属浪费
function openEdit(row) {
  dialogTitle.value = '编辑学生'
  resetForm()

  form.id = row.id
  form.name = row.name
  form.no = row.no
  form.gender = row.gender
  form.phone = row.phone
  form.degree = row.degree
  form.idCard = row.idCard
  form.isCollege = row.isCollege
  form.address = row.address
  form.graduationDate =
    formatDate(row.graduationDate) === '-' ? '' : formatDate(row.graduationDate)
  form.clazzId = row.clazzId

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
    // 只提交表单里的字段。
    //
    // 特别注意：violationCount 和 violationScore 绝对不能带上来。
    // 新增时后端 INSERT 语句把这两列写死了，传 null 进去会撞上数据库的 NOT NULL 直接报错；
    // 修改时后端写的是 <if test="violationCount!=0">，传 0 也改不动它。
    // 违纪数据只走上面的扣分接口，不从这里走。
    const payload = {
      id: form.id,
      name: form.name,
      no: form.no,
      gender: form.gender,
      phone: form.phone,
      degree: form.degree,
      idCard: form.idCard,
      isCollege: form.isCollege,
      address: form.address,
      graduationDate: form.graduationDate || null,
      clazzId: form.clazzId
    }

    if (form.id) {
      await updateStudent(payload)
      ElMessage.success('修改成功')
    } else {
      await addStudent(payload)
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    loadStudents()
  } finally {
    submitting.value = false
  }
}

// ==================== 初始化 ====================

onMounted(() => {
  loadClazzs()
  loadStudents()
})
</script>

<template>
  <div class="student-page">
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

        <el-form-item label="学历">
          <el-select v-model="query.degree" placeholder="全部" clearable class="w-120">
            <el-option
              v-for="item in DEGREE_OPTIONS"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="班级">
          <el-select v-model="query.clazzId" placeholder="全部" clearable class="w-200">
            <el-option
              v-for="item in clazzList"
              :key="item.id"
              :label="item.name"
              :value="item.id"
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
      <div class="toolbar">
        <el-button type="primary" @click="openAdd">
          <el-icon><Plus /></el-icon>
          <span>新增学生</span>
        </el-button>
        <el-button type="danger" :disabled="!selection.length" @click="handleBatchDelete">
          批量删除
        </el-button>
      </div>

      <el-table
        v-loading="loading"
        :data="studentList"
        border
        stripe
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="50" align="center" />
        <el-table-column type="index" label="序号" width="70" align="center" :index="indexMethod" />
        <el-table-column prop="name" label="姓名" width="100" />
        <el-table-column prop="no" label="学号" width="110" />
        <el-table-column label="性别" width="70" align="center">
          <template #default="{ row }">{{ labelOf(GENDER_OPTIONS, row.gender) }}</template>
        </el-table-column>
        <el-table-column prop="phone" label="手机号" width="130" />
        <el-table-column label="学历" width="90" align="center">
          <template #default="{ row }">{{ labelOf(DEGREE_OPTIONS, row.degree) }}</template>
        </el-table-column>
        <el-table-column label="是否院校" width="100" align="center">
          <template #default="{ row }">{{ labelOf(IS_COLLEGE_OPTIONS, row.isCollege) }}</template>
        </el-table-column>
        <el-table-column label="班级" min-width="170">
          <template #default="{ row }">{{ row.clazzName || '-' }}</template>
        </el-table-column>
        <el-table-column prop="violationCount" label="违纪次数" width="100" align="center">
          <template #default="{ row }">{{ row.violationCount ?? 0 }}</template>
        </el-table-column>
        <el-table-column prop="violationScore" label="违纪扣分" width="100" align="center">
          <template #default="{ row }">{{ row.violationScore ?? 0 }}</template>
        </el-table-column>
        <el-table-column label="操作" width="200" align="center" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="openEdit(row)">编辑</el-button>
            <el-button type="warning" link @click="openViolation(row)">违纪扣分</el-button>
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
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="720px" top="6vh">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="姓名" prop="name">
              <el-input v-model="form.name" placeholder="请输入姓名" maxlength="10" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="学号" prop="no">
              <el-input v-model="form.no" placeholder="请输入学号" maxlength="10" />
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
            <el-form-item label="学历">
              <el-select v-model="form.degree" placeholder="请选择" clearable class="w-full">
                <el-option
                  v-for="item in DEGREE_OPTIONS"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="是否院校" prop="isCollege">
              <el-select v-model="form.isCollege" placeholder="请选择" class="w-full">
                <el-option
                  v-for="item in IS_COLLEGE_OPTIONS"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
                />
              </el-select>
            </el-form-item>
          </el-col>

          <el-col :span="24">
            <el-form-item label="身份证号" prop="idCard">
              <el-input v-model="form.idCard" placeholder="请输入 18 位身份证号" maxlength="18" />
            </el-form-item>
          </el-col>

          <el-col :span="12">
            <el-form-item label="毕业时间">
              <el-date-picker
                v-model="form.graduationDate"
                type="date"
                placeholder="请选择日期"
                value-format="YYYY-MM-DD"
                class="w-full"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="班级" prop="clazzId">
              <el-select v-model="form.clazzId" placeholder="请选择" filterable class="w-full">
                <el-option
                  v-for="item in clazzList"
                  :key="item.id"
                  :label="item.name"
                  :value="item.id"
                />
              </el-select>
            </el-form-item>
          </el-col>

          <el-col :span="24">
            <el-form-item label="联系地址">
              <el-input
                v-model="form.address"
                type="textarea"
                :rows="2"
                placeholder="请输入联系地址"
                maxlength="100"
              />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- 违纪扣分弹窗 -->
    <el-dialog v-model="violationVisible" title="违纪扣分" width="420px">
      <el-form label-width="90px">
        <el-form-item label="学生">
          <span>{{ violationForm.name }}</span>
        </el-form-item>
        <el-form-item label="扣分数">
          <el-input-number v-model="violationForm.score" :min="1" :max="100" class="w-full" />
        </el-form-item>
        <el-form-item>
          <div class="violation-tip">
            扣分后违纪次数会加 1；如果剩余分数不够扣，会直接归零。
          </div>
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="violationVisible = false">取消</el-button>
        <el-button type="primary" :loading="violationSubmitting" @click="handleViolation">
          确定
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.student-page {
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
.w-120 {
  width: 120px;
}
.w-200 {
  width: 200px;
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

.violation-tip {
  font-size: 12px;
  color: #909399;
  line-height: 1.6;
}
</style>
