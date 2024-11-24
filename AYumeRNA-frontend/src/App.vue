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
import { ref } from 'vue';
import AppSidebar from './components/AppSidebar.vue';

// 控制侧边栏展开/收起的状态
const isCollapsed = ref(false);

// 切换侧边栏的状态
function toggleSidebar() {
  isCollapsed.value = !isCollapsed.value;
}
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
  flex-grow: 1;
/* 主内容区域可滚动 */
  transition: margin-left 0.3s ease; /* 控制主内容区域的过渡 */
}

.content {
  flex: 1;
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
