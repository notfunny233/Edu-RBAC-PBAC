<template>
  <div class="page">
    <div v-loading="loading" class="chart-grid">
      <!-- 职位分布 -->
      <el-card shadow="never" class="chart-card">
        <template #header>
          <div class="card-head">
            <div>
              <span class="card-title">员工职位分布</span>
              <span class="card-note">没填职位的员工归入「其他」</span>
            </div>
            <span v-if="jobTotal !== null" class="card-total">共 {{ jobTotal }} 人</span>
          </div>
        </template>
        <div ref="jobRef" class="chart"></div>
      </el-card>

      <!-- 性别分布 -->
      <el-card shadow="never" class="chart-card">
        <template #header>
          <div class="card-head">
            <div>
              <span class="card-title">员工性别分布</span>
            </div>
            <span v-if="genderTotal !== null" class="card-total">共 {{ genderTotal }} 人</span>
          </div>
        </template>
        <div ref="genderRef" class="chart"></div>
      </el-card>

      <!-- 学历分布 -->
      <el-card shadow="never" class="chart-card">
        <template #header>
          <div class="card-head">
            <div>
              <span class="card-title">学员学历分布</span>
            </div>
            <span v-if="degreeTotal !== null" class="card-total">共 {{ degreeTotal }} 人</span>
          </div>
        </template>
        <div ref="degreeRef" class="chart"></div>
      </el-card>

      <!-- 班级学员数 -->
      <el-card shadow="never" class="chart-card">
        <template #header>
          <div class="card-head">
            <div>
              <span class="card-title">各班级学员人数</span>
              <span class="card-note">没有学员的班级显示为 0</span>
            </div>
            <span v-if="countTotal !== null" class="card-total">共 {{ countTotal }} 人</span>
          </div>
        </template>
        <div ref="countRef" class="chart"></div>
      </el-card>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount } from 'vue'
import * as echarts from 'echarts'
import {
  getEmpJobData,
  getEmpGenderData,
  getStudentDegreeData,
  getStudentCountData
} from '@/api/report'

const loading = ref(false)

// 每个图表都配一个"共 N 人"。后端没返回总数，
// 但这个数能从数据里加出来，摆在卡片右上角方便一眼核对数据是否完整
const jobTotal = ref(null)
const genderTotal = ref(null)
const degreeTotal = ref(null)
const countTotal = ref(null)

const jobRef = ref(null)
const genderRef = ref(null)
const degreeRef = ref(null)
const countRef = ref(null)

// ECharts 实例只是一个画布句柄，不需要响应式。
// 用普通变量存，不要放进 ref——放 ref 里 Vue 会去深度代理整个图表对象，白费性能
let jobChart = null
let genderChart = null
let degreeChart = null
let countChart = null

// 图表里的文字和线条颜色。写死成深色是因为页面是浅色主题，
// ECharts 默认的配色偏灰，在浅色背景上不够清楚
const TEXT_COLOR = '#303133'
const SUB_TEXT_COLOR = '#909399'
const AXIS_LINE_COLOR = '#dcdfe6'
const SPLIT_LINE_COLOR = '#ebeef5'

// 判断一组数字里有没有有效数据。
// 全是 0 也算没数据——那样画出来是一张空图，不如直接写"暂无数据"清楚
function hasData(values) {
  if (!Array.isArray(values) || values.length === 0) return false
  return values.some((v) => Number(v) > 0)
}

// 数据为空时画一句"暂无数据"，别留一张空白画布让用户以为页面坏了
function renderEmpty(chart) {
  chart.clear()
  chart.setOption({
    title: {
      text: '暂无数据',
      left: 'center',
      top: 'center',
      textStyle: { color: SUB_TEXT_COLOR, fontSize: 14, fontWeight: 'normal' }
    }
  })
}

// 把一组数字加起来，给卡片右上角用
function sum(values) {
  return values.reduce((acc, v) => acc + (Number(v) || 0), 0)
}

// ---------- 图表 1：员工职位分布（竖向柱状）----------
// 后端返回 { jobList: ['班主任', ...], dataList: [6, 11, ...] }，两个平行数组
function renderJob(data) {
  const names = data?.jobList || []
  const values = data?.dataList || []

  if (!names.length || !hasData(values)) {
    jobTotal.value = null
    return renderEmpty(jobChart)
  }

  jobTotal.value = sum(values)

  jobChart.setOption({
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
      formatter: '{b}：{c} 人'
    },
    grid: { left: 10, right: 16, top: 40, bottom: 10, containLabel: true },
    xAxis: {
      type: 'category',
      data: names,
      axisLine: { lineStyle: { color: AXIS_LINE_COLOR } },
      axisTick: { show: false },
      axisLabel: { color: TEXT_COLOR, interval: 0 }
    },
    yAxis: {
      type: 'value',
      // minInterval 必须是 1。不加的话人数这种整数会被 y 轴拆成 0.5、1.5 这种刻度，
      // 看上去很怪（"2.5 个人"）
      minInterval: 1,
      axisLine: { show: false },
      axisTick: { show: false },
      axisLabel: { color: SUB_TEXT_COLOR },
      splitLine: { lineStyle: { color: SPLIT_LINE_COLOR } }
    },
    series: [
      {
        type: 'bar',
        data: values,
        barMaxWidth: 48,
        itemStyle: { color: '#409eff', borderRadius: [4, 4, 0, 0] },
        // 柱顶直接标出人数，省得非要鼠标移上去才知道是多少
        label: { show: true, position: 'top', color: TEXT_COLOR }
      }
    ]
  })
}

// ---------- 图表 2：员工性别分布（环形饼图）----------
// 后端返回 [{ name: '男性员工', value: 25 }, ...]
function renderGender(data) {
  const list = data || []

  if (!list.length || !hasData(list.map((i) => i.value))) {
    genderTotal.value = null
    return renderEmpty(genderChart)
  }

  genderTotal.value = sum(list.map((i) => i.value))

  genderChart.setOption({
    tooltip: {
      trigger: 'item',
      formatter: '{b}：{c} 人（{d}%）'
    },
    legend: {
      bottom: 0,
      icon: 'circle',
      textStyle: { color: TEXT_COLOR }
    },
    // 颜色按后端给的名称固定分配：男的用蓝、女的用红。
    // 不固定的话，万一某天性别数据只剩一种，颜色会跟着变，容易让人看错
    color: genderColorOf(list),
    series: [
      {
        type: 'pie',
        radius: ['45%', '68%'],
        center: ['50%', '45%'],
        data: list,
        label: { formatter: '{b}\n{c} 人', color: TEXT_COLOR },
        labelLine: { lineStyle: { color: AXIS_LINE_COLOR } },
        itemStyle: { borderColor: '#fff', borderWidth: 2 }
      }
    ]
  })
}

// 按名称挑颜色。"男性"给蓝色、"女性"给粉红，其余用灰色兜底
function genderColorOf(list) {
  return list.map((item) => {
    if (item.name && item.name.includes('男')) return '#409eff'
    if (item.name && item.name.includes('女')) return '#f78989'
    return '#909399'
  })
}

// ---------- 图表 3：学员学历分布（环形饼图）----------
// 后端返回 [{ name: '本科', value: 9 }, ...]
//
// 注意：后端那条统计 SQL 只写到"硕士"，
// 一旦有学生学历选到 6（博士），这条数据的 name 会是 null，
// 图上就会出现一个没有名字的扇区。真遇到了要回后端补 SQL，不是前端能补的
function renderDegree(data) {
  const list = (data || []).map((item) => ({
    // name 为 null 时给个占位文字，总比空白扇区强
    name: item.name || '未知学历',
    value: item.value
  }))

  if (!list.length || !hasData(list.map((i) => i.value))) {
    degreeTotal.value = null
    return renderEmpty(degreeChart)
  }

  degreeTotal.value = sum(list.map((i) => i.value))

  degreeChart.setOption({
    tooltip: {
      trigger: 'item',
      formatter: '{b}：{c} 人（{d}%）'
    },
    legend: {
      bottom: 0,
      icon: 'circle',
      textStyle: { color: TEXT_COLOR }
    },
    // 学历是从低到高排的，用同一色系由浅到深，一眼能看出递进关系
    color: ['#c8e0ff', '#a0cfff', '#79bbff', '#409eff', '#337ecc', '#1d5fa8'],
    series: [
      {
        type: 'pie',
        radius: ['45%', '68%'],
        center: ['50%', '45%'],
        data: list,
        label: { formatter: '{b}\n{c} 人', color: TEXT_COLOR },
        labelLine: { lineStyle: { color: AXIS_LINE_COLOR } },
        itemStyle: { borderColor: '#fff', borderWidth: 2 }
      }
    ]
  })
}

// ---------- 图表 4：各班级学员人数（横向柱状）----------
// 后端返回 { name: ['JavaEE就业163期', ...], value: [7, 2, ...] }
//
// 用横向是因为班级名字长，竖着排会挤成一团或者被斜着切掉
function renderCount(data) {
  const names = data?.name || []
  const values = data?.value || []

  if (!names.length) {
    countTotal.value = null
    return renderEmpty(countChart)
  }

  // 这个图跟别的不一样：班级没人时是 0，**不能**当成"没数据"处理，
  // 因为"这个班还没招到人"本身就是一条要看的报表信息。
  // 所以这里不用 hasData 判断，只要班级列表非空就画
  countTotal.value = sum(values)

  countChart.setOption({
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
      formatter: '{b}：{c} 人'
    },
    grid: { left: 10, right: 40, top: 20, bottom: 10, containLabel: true },
    xAxis: {
      type: 'value',
      minInterval: 1,
      axisLine: { show: false },
      axisTick: { show: false },
      axisLabel: { color: SUB_TEXT_COLOR },
      splitLine: { lineStyle: { color: SPLIT_LINE_COLOR } }
    },
    yAxis: {
      type: 'category',
      data: names,
      // 横轴的类目从下往上排，反转一下让第一个班级显示在最上面，符合阅读习惯
      inverse: true,
      axisLine: { lineStyle: { color: AXIS_LINE_COLOR } },
      axisTick: { show: false },
      axisLabel: { color: TEXT_COLOR }
    },
    series: [
      {
        type: 'bar',
        data: values,
        barMaxWidth: 28,
        itemStyle: { color: '#67c23a', borderRadius: [0, 4, 4, 0] },
        label: { show: true, position: 'right', color: TEXT_COLOR }
      }
    ]
  })
}

// ---------- 加载数据 ----------
async function loadAll() {
  loading.value = true
  try {
    // 用 allSettled 而不是 all：四个报表各自对应一个权限点，
    // 万一某个没权限，也不该把其他三个能看的图一起拖垮。
    // 失败的请求 response 拦截器会自己弹提示，这里不用重复处理
    const [job, gender, degree, count] = await Promise.allSettled([
      getEmpJobData(),
      getEmpGenderData(),
      getStudentDegreeData(),
      getStudentCountData()
    ])

    if (job.status === 'fulfilled') renderJob(job.value.data)
    if (gender.status === 'fulfilled') renderGender(gender.value.data)
    if (degree.status === 'fulfilled') renderDegree(degree.value.data)
    if (count.status === 'fulfilled') renderCount(count.value.data)
  } finally {
    loading.value = false
  }
}

// 浏览器窗口变窄时图表不会自动跟着缩，得手动通知它重画
function handleResize() {
  jobChart?.resize()
  genderChart?.resize()
  degreeChart?.resize()
  countChart?.resize()
}

onMounted(() => {
  jobChart = echarts.init(jobRef.value)
  genderChart = echarts.init(genderRef.value)
  degreeChart = echarts.init(degreeRef.value)
  countChart = echarts.init(countRef.value)

  window.addEventListener('resize', handleResize)

  loadAll()
})

// 离开这个页面时必须销毁图表实例，否则它注册的监听和 canvas 会一直留在内存里，
// 反复进出几次报表页就会明显变卡
onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  jobChart?.dispose()
  genderChart?.dispose()
  degreeChart?.dispose()
  countChart?.dispose()
})
</script>

<style scoped>
.chart-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
}

/* 窗口窄的时候并排两个图会挤得看不清，退成一列 */
@media (max-width: 1100px) {
  .chart-grid {
    grid-template-columns: 1fr;
  }
}

.chart-card :deep(.el-card__header) {
  padding: 14px 20px;
}

.card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.card-title {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
}

.card-note {
  margin-left: 8px;
  font-size: 12px;
  color: #a8abb2;
}

.card-total {
  font-size: 13px;
  color: #909399;
}

.chart {
  height: 320px;
}
</style>
