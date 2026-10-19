<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { menus } from '@/config/menu'

defineProps({
  // 是否折叠。折叠后只剩图标，宽度由外面的 layout 控制
  collapse: {
    type: Boolean,
    default: false
  }
})

const route = useRoute()

// 当前高亮哪一项。直接用当前路由路径去比，菜单项的 index 就是它的 path。
const activeMenu = computed(() => route.path)
</script>

<template>
  <!-- router 这个属性很关键：点了菜单项，el-menu 会自己按 index 去跳路由，
       不用我们再写一遍点击事件 -->
  <el-menu
    class="sidebar-menu"
    :default-active="activeMenu"
    :collapse="collapse"
    :collapse-transition="false"
    router
  >
    <template v-for="item in menus" :key="item.path || item.title">
      <!-- 有 children 的渲染成可展开的子菜单 -->
      <el-sub-menu v-if="item.children" :index="item.title">
        <template #title>
          <el-icon><component :is="item.icon" /></el-icon>
          <span>{{ item.title }}</span>
        </template>
        <el-menu-item
          v-for="child in item.children"
          :key="child.path"
          :index="child.path"
        >
          {{ child.title }}
        </el-menu-item>
      </el-sub-menu>

      <!-- 没有 children 的就是普通一项 -->
      <el-menu-item v-else :index="item.path">
        <el-icon><component :is="item.icon" /></el-icon>
        <template #title>{{ item.title }}</template>
      </el-menu-item>
    </template>
  </el-menu>
</template>

<style scoped>
/* Element Plus 2.x 推荐用 CSS 变量改菜单配色，
   老写法（直接在 el-menu 上写 background-color 属性）已经废弃了。
   这几行把它改成深色侧边栏的样子。 */
.sidebar-menu {
  --el-menu-bg-color: transparent;
  --el-menu-text-color: #c0c4cc;
  --el-menu-hover-bg-color: #3a465c;
  --el-menu-active-color: #ffffff;
  border-right: none;
  flex: 1;
  overflow-y: auto;
  overflow-x: hidden;
}

/* 选中的那一项给个明显的底色，不然深色背景下只看文字色差不太出来 */
.sidebar-menu :deep(.el-menu-item.is-active) {
  background-color: #409eff;
}

.sidebar-menu :deep(.el-sub-menu .el-menu-item) {
  background-color: #232c3b;
}
</style>
