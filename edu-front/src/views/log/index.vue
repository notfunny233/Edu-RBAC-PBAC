<template>
  <div class="page">
    <el-alert
      type="info"
      :closable="false"
      show-icon
      class="tip"
      title="日志接口只提供分页，不支持按操作人或时间筛选，所以这里没有查询条件。列表按操作时间倒序，最新的在最前面。"
    />

    <el-card shadow="never">
      <el-table v-loading="loading" :data="list" stripe>
        <el-table-column prop="id" label="ID" width="80" />

        <el-table-column label="操作人" width="130">
          <template #default="{ row }">{{ operatorName(row) }}</template>
        </el-table-column>

        <el-table-column label="操作时间" width="180">
          <template #default="{ row }">{{ formatDateTime(row.operateTime) }}</template>
        </el-table-column>

        <!-- class_name 存的是全限定类名（example.Controller.DeptController），
             整串显示会把表格撑得很宽，这里只取最后一段类名 -->
        <el-table-column label="操作类" min-width="180">
          <template #default="{ row }">{{ shortClass(row.className) }}</template>
        </el-table-column>

        <el-table-column prop="methodName" label="方法" min-width="160" />

        <el-table-column label="耗时" width="100" align="right">
          <template #default="{ row }">
            {{ row.costTime === null || row.costTime === undefined ? '-' : row.costTime + ' ms' }}
          </template>
        </el-table-column>

        <el-table-column label="参数 / 返回值" width="130" align="center">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row)">查看</el-button>
          </template>
        </el-table-column>

        <template #empty>
          <el-empty description="暂无操作日志" />
        </template>
      </el-table>

      <el-pagination
        v-model:current-page="currentPage"
        v-model:page-size="pageSize"
        :page-sizes="[10, 20, 50]"
        :total="total"
        layout="total, sizes, prev, pager, next, jumper"
        class="pager"
        @size-change="handleSizeChange"
        @current-change="loadLogs"
      />
    </el-card>

    <!-- 参数和返回值在数据库里各占 2000 字符，塞进表格会把行撑爆，
         所以单独开个弹窗看 -->
    <el-dialog v-model="detailVisible" title="操作详情" width="760px">
      <el-descriptions v-if="detailRow" :column="2" border>
        <el-descriptions-item label="操作人">{{ operatorName(detailRow) }}</el-descriptions-item>
        <el-descriptions-item label="操作时间">
          {{ formatDateTime(detailRow.operateTime) }}
        </el-descriptions-item>
        <el-descriptions-item label="操作类">{{ detailRow.className || '-' }}</el-descriptions-item>
        <el-descriptions-item label="方法">{{ detailRow.methodName || '-' }}</el-descriptions-item>
      </el-descriptions>

      <div class="detail-block">
        <div class="detail-label">方法参数</div>
        <pre class="detail-code">{{ prettyJson(detailRow?.methodParams) }}</pre>
      </div>

      <div class="detail-block">
        <div class="detail-label">返回值</div>
        <pre class="detail-code">{{ prettyJson(detailRow?.returnValue) }}</pre>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { pageLogs } from '@/api/log'
import { listAllEmps } from '@/api/emp'
import { formatDateTime } from '@/utils/format'

const loading = ref(false)
const list = ref([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)

// 员工 id 到姓名的对应表。
// 日志表里只存了 operate_emp_id，没有姓名，所以要么显示一串数字，
// 要么额外拉一次员工列表在本地对一下。这里选了后者
const empNameMap = ref({})

const detailVisible = ref(false)
const detailRow = ref(null)

async function loadLogs() {
  loading.value = true
  try {
    // ⚠️ 每页条数的参数名是 pagesize（全小写），不是其他模块的 pageSize。
    // 写成 pageSize 不会报错，只是后端收不到、改用默认值 10——静默失效
    const res = await pageLogs({
      page: currentPage.value,
      pagesize: pageSize.value
    })
    list.value = res.data.rows || []
    total.value = res.data.total || 0
  } finally {
    loading.value = false
  }
}

// 拉一遍员工列表，把 id 换成姓名。
// 这个接口要 emp:list 权限，只有 log:list 权限的人是拉不到的——
// 那不该影响看日志，所以失败就安静跳过，操作人那一列退化成显示"员工 #id"
async function loadEmpNames() {
  try {
    const res = await listAllEmps()
    const map = {}
    ;(res.data || []).forEach((emp) => {
      map[emp.id] = emp.name
    })
    empNameMap.value = map
  } catch {
    // 没权限就跳过，不用弹提示打扰用户
  }
}

function operatorName(row) {
  const id = row?.operateEmpId
  if (id === null || id === undefined) return '（未知）'

  const name = empNameMap.value[id]
  return name ? name : `员工 #${id}`
}

function shortClass(className) {
  if (!className) return '-'
  const parts = String(className).split('.')
  return parts[parts.length - 1]
}

// 后端存的是 Java 对象的 toString，不保证是合法 JSON。
// 能解析就缩进美化，解析不了就原样显示——不要为了"好看"把内容弄丢
function prettyJson(text) {
  if (!text) return '（空）'
  try {
    return JSON.stringify(JSON.parse(text), null, 2)
  } catch {
    return String(text)
  }
}

function openDetail(row) {
  detailRow.value = row
  detailVisible.value = true
}

// 改了每页条数要回到第 1 页，不然会停在一个已经不存在的页码上（比如原来在第 5 页，
// 改成每页 50 条后总共只剩 1 页），表格会是空的
function handleSizeChange() {
  currentPage.value = 1
  loadLogs()
}

onMounted(() => {
  loadLogs()
  loadEmpNames()
})
</script>

<style scoped>
.tip {
  margin-bottom: 16px;
}

.pager {
  margin-top: 16px;
  justify-content: flex-end;
}

.detail-block {
  margin-top: 16px;
}

.detail-label {
  margin-bottom: 6px;
  font-size: 13px;
  color: #606266;
}

/* 参数可能很长，用等宽字体 + 自动换行，别让内容横向溢出到看不见 */
.detail-code {
  max-height: 220px;
  padding: 12px;
  margin: 0;
  overflow: auto;
  font-family: Consolas, Monaco, monospace;
  font-size: 13px;
  line-height: 1.6;
  color: #303133;
  word-break: break-all;
  white-space: pre-wrap;
  background: #f5f7fa;
  border-radius: 4px;
}
</style>
