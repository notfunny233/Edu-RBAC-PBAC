<script setup>
import { useRoute } from 'vue-router'

// 各模块的临时占位页。
// 页面上显示的标题从路由的 meta.title 取，所以一份代码能顶所有模块。
// 以后做某个模块的真实页面时，去 router/index.js 把它的 component 改成
// 自己的文件（比如 @/views/dept/index.vue），这个占位页就不用管了。
const route = useRoute()
</script>

<template>
  <el-card shadow="never">
    <el-empty :description="`「${route.meta.title || '该模块'}」还没开始做`">
      <div class="hint">
        这个模块的后端接口已经就绪，等前面几个模块的页面做完了再回来补。
      </div>
    </el-empty>
  </el-card>
</template>

<style scoped>
.hint {
  font-size: 13px;
  color: #909399;
}
</style>
