<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { pageClazzs, addClazz, updateClazz, deleteClazz } from '@/api/clazz'
import { listAllEmps } from '@/api/emp'
import { SUBJECT_OPTIONS, labelOf } from '@/config/dict'
import { formatDate } from '@/utils/format'

// ==================== 查询条件 ====================

const query = reactive({
  name: '',
  // 开课时间范围先用数组接住，提交时再拆成 begin 和 end 两个参数。
  // 用 daterange 控件的好处是它要么给两个日期、要么一个都不给，
  // 正好满足后端"begin 和 end 必须同时传"的要求
  dateRange: []
})

// ==================== 分页列表 ====================

const loading = ref(false)
const clazzList = ref([])
const total = ref(0)
const currentPage = ref(1)
// 后端这个接口的 pageSize 默认值是 5，这里显式给 10。
// 不传的话页面上会显示"一共 5 条"，看着像数据丢了
const pageSize = ref(10)

// 状态是后端在 SQL 里用 now() 现算的，不是数据库里的字段，
// 这里只负责挑个颜色显示：
// 未开始=灰，已开始=绿，已结束=橙
const STATUS_TAG = {
  未开始: 'info',
  已开始: 'success',
  已结束: 'warning'
}

async function loadClazzs() {
  loading.value = true
  try {
    const [begin, end] = query.dateRange || []
    const res = await pageClazzs({
      page: currentPage.value,
      pageSize: pageSize.value,
      name: query.name || undefined,
      begin: begin || undefined,
      end: end || undefined
    })
    clazzList.value = res.data.rows || []
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
  // 条件变了必须回到第一页，否则在第 5 页筛数据、筛完只剩 1 页，
  // 就会看到一张空表格，还以为是"没查到"
  currentPage.value = 1
  loadClazzs()
}

function handleReset() {
  query.name = ''
  query.dateRange = []
  handleSearch()
}

function handlePageChange(page) {
  currentPage.value = page
  loadClazzs()
}

function handleSizeChange(size) {
  pageSize.value = size
  currentPage.value = 1
  loadClazzs()
}

// ==================== 删除 ====================
//
// 班级只有单个删除接口，没有批量删除，所以表格上没有勾选框。
// 这是后端接口就这么设计的，不是漏做了。

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(
      `确定要删除「${row.name}」吗？如果班里还有学生，删除后他们会失去班级归属。`,
      '提示',
      { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' }
    )
  } catch {
    return
  }
  await deleteClazz(row.id)
  ElMessage.success('删除成功')
  loadClazzs()
}

// ==================== 班主任下拉 ====================

const empList = ref([])

async function loadEmps() {
  const res = await listAllEmps()
  empList.value = res.data || []
}

// ==================== 新增 / 编辑弹窗 ====================

const dialogVisible = ref(false)
const dialogTitle = ref('')
const submitting = ref(false)
const formRef = ref()

const form = reactive({
  id: null,
  name: '',
  room: '',
  beginDate: '',
  endDate: '',
  masterId: null,
  subject: null
})

// 结课时间不能早于开课时间。
// 数据库里已经躺着两条倒着的班级了（开课 2024-07-10、结课 2024-01-20），
// 加这条是为了别再生成新的；改那两条老数据时也会被拦一下，那是提醒你去修它
function checkEndDate(rule, value, callback) {
  if (value && form.beginDate && value < form.beginDate) {
    callback(new Error('结课时间不能早于开课时间'))
    return
  }
  callback()
}

const rules = {
  name: [{ required: true, message: '请输入班级名称', trigger: 'blur' }],
  beginDate: [{ required: true, message: '请选择开课时间', trigger: 'change' }],
  endDate: [
    { required: true, message: '请选择结课时间', trigger: 'change' },
    { validator: checkEndDate, trigger: 'change' }
  ],
  subject: [{ required: true, message: '请选择学科', trigger: 'change' }]
}

function resetForm() {
  form.id = null
  form.name = ''
  form.room = ''
  form.beginDate = ''
  form.endDate = ''
  form.masterId = null
  form.subject = null
}

function openAdd() {
  dialogTitle.value = '新增班级'
  resetForm()
  dialogVisible.value = true
}

// 编辑直接用表格里那一行数据填，不再请求详情接口。
// 原因：列表查询用的是 select c.*，编辑要用的字段一个不少，多请求一次纯属浪费。
// （员工模块是例外，那边的工作经历只存在于详情接口里，所以必须重新请求）
function openEdit(row) {
  dialogTitle.value = '编辑班级'
  resetForm()

  form.id = row.id
  form.name = row.name
  form.room = row.room
  form.beginDate = formatDate(row.beginDate) === '-' ? '' : formatDate(row.beginDate)
  form.endDate = formatDate(row.endDate) === '-' ? '' : formatDate(row.endDate)
  form.masterId = row.masterId
  form.subject = row.subject

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
    // 只提交后端认识的字段。
    // status 和 masterName 是查询时算出来的，传上去也没用，干脆一开始就不带
    const payload = {
      id: form.id,
      name: form.name,
      room: form.room,
      beginDate: form.beginDate,
      endDate: form.endDate,
      masterId: form.masterId,
      subject: form.subject
    }

    if (form.id) {
      await updateClazz(payload)
      ElMessage.success('修改成功')
    } else {
      await addClazz(payload)
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    loadClazzs()
  } finally {
    submitting.value = false
  }
}

// ==================== 初始化 ====================

onMounted(() => {
  loadEmps()
  loadClazzs()
})
</script>

<template>
  <div class="clazz-page">
    <!-- 查询条件 -->
    <el-card shadow="never" class="search-card">
      <el-form :model="query" inline>
        <el-form-item label="班级名称">
          <el-input
            v-model="query.name"
            placeholder="请输入班级名称"
            clearable
            class="w-180"
            @keyup.enter="handleSearch"
          />
        </el-form-item>

        <el-form-item label="开课时间">
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
          <span>新增班级</span>
        </el-button>
      </div>

      <el-table v-loading="loading" :data="clazzList" border stripe>
        <el-table-column type="index" label="序号" width="70" align="center" :index="indexMethod" />
        <el-table-column prop="name" label="班级名称" min-width="180" />
        <el-table-column prop="room" label="班级教室" width="110" align="center" />
        <el-table-column label="开课时间" width="120" align="center">
          <template #default="{ row }">{{ formatDate(row.beginDate) }}</template>
        </el-table-column>
        <el-table-column label="结课时间" width="120" align="center">
          <template #default="{ row }">{{ formatDate(row.endDate) }}</template>
        </el-table-column>
        <el-table-column prop="masterName" label="班主任" width="110">
          <template #default="{ row }">{{ row.masterName || '-' }}</template>
        </el-table-column>
        <el-table-column label="学科" width="110" align="center">
          <template #default="{ row }">{{ labelOf(SUBJECT_OPTIONS, row.subject) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="STATUS_TAG[row.status] || 'info'">{{ row.status || '-' }}</el-tag>
          </template>
        </el-table-column>
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
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="620px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-row :gutter="16">
          <el-col :span="24">
            <el-form-item label="班级名称" prop="name">
              <el-input v-model="form.name" placeholder="请输入班级名称" maxlength="30" />
            </el-form-item>
          </el-col>

          <el-col :span="12">
            <el-form-item label="班级教室">
              <el-input v-model="form.room" placeholder="如 212" maxlength="20" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="学科" prop="subject">
              <el-select v-model="form.subject" placeholder="请选择" class="w-full">
                <el-option
                  v-for="item in SUBJECT_OPTIONS"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
                />
              </el-select>
            </el-form-item>
          </el-col>

          <el-col :span="12">
            <el-form-item label="开课时间" prop="beginDate">
              <el-date-picker
                v-model="form.beginDate"
                type="date"
                placeholder="请选择日期"
                value-format="YYYY-MM-DD"
                class="w-full"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="结课时间" prop="endDate">
              <el-date-picker
                v-model="form.endDate"
                type="date"
                placeholder="请选择日期"
                value-format="YYYY-MM-DD"
                class="w-full"
              />
            </el-form-item>
          </el-col>

          <el-col :span="24">
            <el-form-item label="班主任">
              <el-select
                v-model="form.masterId"
                placeholder="请选择"
                clearable
                filterable
                class="w-full"
              >
                <el-option
                  v-for="item in empList"
                  :key="item.id"
                  :label="item.name"
                  :value="item.id"
                />
              </el-select>
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
.clazz-page {
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
</style>
