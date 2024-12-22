<!-- src/App.vue -->
<template>
  <div id="app" class="d-flex flex-column min-vh-100">
    <div class="row flex-grow-1">
      <!-- 侧边栏组件 -->
      <div :class="['col-auto', { 'col-2': !isCollapsed, 'col-1': isCollapsed }]" id="sidebar">
        <AppSidebar :isCollapsed="isCollapsed" @toggle="toggleSidebar" />
      </div>

      <!-- 主内容区域 -->
      <div :class="['col', 'd-flex', 'flex-column']" id="main-content" :style="{ marginLeft: isCollapsed ? '80px' : '240px' }">
        <main class="content container-fluid">
          <router-view />
        </main>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue';
import { useRoute } from 'vue-router';
import AppSidebar from './components/AppSidebar.vue';

// 导入 ref
const isCollapsed = ref(false);
const route = useRoute();

function toggleSidebar() {
  isCollapsed.value = !isCollapsed.value;
}

// 监听路由变化并刷新页面，避免无限刷新
watch(route, (newRoute, oldRoute) => {
  console.log(`Route changed from ${String(oldRoute.name)} to ${String(newRoute.name)}`);

  // 仅在导航到 /CodonGenerator 时触发刷新
  if (newRoute.path === '/CodonGenerator') {
    // 检查是否已经刷新过
    const hasRefreshed = localStorage.getItem('codonGeneratorRefreshed');

    if (!hasRefreshed) {
      // 设置标记表示已刷新
      localStorage.setItem('codonGeneratorRefreshed', 'true');
      console.log('Refreshing the page for /CodonGenerator...');
      window.location.reload();
    } else {
      // 清除标记，允许下一次刷新
      localStorage.removeItem('codonGeneratorRefreshed');
      console.log('/CodonGenerator has already been refreshed once.');
    }
  }
});
</script>

<style scoped>
/* 使应用背景为渐变 */
#app {
  background: linear-gradient(to right, #f0f4f8, #d9e2ec);
  display: flex;
  flex-direction: row; /* 页面布局为横向 */
  height: 100vh; /* 保持全屏 */
  overflow-x: hidden;
}

/* 侧边栏的样式 */
#sidebar {
  position: fixed; /* 固定在左侧 */
  top: 0;
  bottom: 0;
  left: 0;
  width: 240px; /* 默认宽度 */
  z-index: 1000; /* 保证侧边栏在最上层 */
  transition: width 0.3s ease;
  background-color: #2c3e50; /* 侧边栏背景 */
}

#sidebar.col-1 {
  width: 80px; /* 折叠后宽度 */
}

#main-content {
  margin-left: 240px; /* 默认侧边栏宽度 */
  transition: margin-left 0.3s ease; /* 控制主内容区域的过渡 */
  flex-grow: 1;
}

#main-content.col-1 {
  margin-left: 80px; /* 折叠后侧边栏宽度 */
}

.content {
  padding: 20px; /* 为内容添加内边距 */
}

@media (max-width: 768px) {
  #sidebar {
    width: 80px; /* 窄侧边栏宽度 */
  }

  #main-content {
    margin-left: 80px; /* 内容区域相应调整 */
  }
}
</style>
