<script setup>
// 整个应用的出口。
// 这里只放一个 router-view，具体显示登录页还是主界面由路由决定。
</script>

<template>
  <router-view />
</template>

<style>
/* 清掉浏览器默认的边距，否则页面四周会有难看的白边 */
html,
body,
#app {
  height: 100%;
  margin: 0;
  padding: 0;
}

body {
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', 'Microsoft YaHei',
    'PingFang SC', sans-serif;
}
</style>
