import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'

import 'element-plus/dist/index.css'
import App from './App.vue'
import router from './router'

const app = createApp(App)

// 把 Element Plus 的图标全部注册成全局组件，
// 这样模板里直接写 <el-icon><User /></el-icon> 就能用，不用逐个 import。
// 菜单配置里写的图标名字（比如 'User'）也是靠这里才能找到对应组件。
for (const [name, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(name, component)
}

app.use(createPinia())
app.use(router)
// 指定中文语言包，分页器、日期选择器、确认框这些内置文字才会是中文
app.use(ElementPlus, { locale: zhCn })

app.mount('#app')
