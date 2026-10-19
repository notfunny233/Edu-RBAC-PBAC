// 后端的时间字段（LocalDateTime）传过来是 ISO 格式，长这样：2026-09-18T21:00:00
// 中间那个 T 是国际标准的分隔符，直接显示很丑，换成空格就正常了。
// 空值统一显示成「-」，免得表格里出现一片空白。
export function formatDateTime(value) {
  if (!value) return '-'
  return String(value).replace('T', ' ')
}

// 日期只要到天，比如 2026-09-18
//
// 后端有两种日期类型，传过来的样子不一样：
// - LocalDateTime（比如创建时间）是 ISO 字符串：2026-09-18T00:00:00
// - java.util.Date（学生的毕业时间用的就是它）有可能是 ISO 字符串，
//   也有可能是一串毫秒数：1758211200000
// 所以这里两种都兜一下，不然表格里会冒出一长串看不懂的数字
export function formatDate(value) {
  if (!value) return '-'

  if (typeof value === 'number') {
    const d = new Date(value)
    const month = String(d.getMonth() + 1).padStart(2, '0')
    const day = String(d.getDate()).padStart(2, '0')
    return `${d.getFullYear()}-${month}-${day}`
  }

  return String(value).slice(0, 10)
}

// 日期区间的右端点，要往后挪一天再传给后端。
//
// 为什么：后端的 end 是"日期"（LocalDate），'2026-09-18' 在 MySQL 里等于
// '2026-09-18 00:00:00'，而 create_time 这种字段是**带时分秒**的。
// 直接写 between '2026-09-01' and '2026-09-18'，会把 9 月 18 日 00:00 之后的记录
// 全部排除掉——用户看到的现象是"最后一天的数据莫名其妙少了"。
// 往后挪一天，9 月 18 日一整天的记录就都能落在区间里了。
//
// 传进来的必须是 'YYYY-MM-DD' 格式（el-date-picker 设了 value-format，保证是这个格式）
export function nextDay(value) {
  if (!value) return undefined
  const date = new Date(`${value}T00:00:00`)
  if (Number.isNaN(date.getTime())) return undefined

  date.setDate(date.getDate() + 1)

  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${date.getFullYear()}-${month}-${day}`
}
