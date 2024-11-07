<template>
  <div id="app">
    <AppSidebar :isCollapsed="isCollapsed" @toggle="toggleSidebar" />
    <div :class="['main-content', { collapsed: isCollapsed }]">
      <main class="content">
        <router-view />
      </main>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import AppSidebar from './components/AppSidebar.vue';

const isCollapsed = ref(false);

function toggleSidebar() {
  isCollapsed.value = !isCollapsed.value;
}
</script>

<style scoped>
#app {
  display: flex;
  flex-direction: column;
  min-height: 100vh; /* 确保填满视口高度 */
  width: 100%;
  background: linear-gradient(to right, #f0f4f8, #d9e2ec);
  overflow: hidden; /* 防止父容器滚动条干扰 */
}

/* 动态调整右侧内容区域的左边距 */
.main-content {
  display: flex;
  flex-direction: column;

  position: relative; /* 改为相对定位，这样内容能动态扩展 */
  top: 0;
  bottom: 0;
  left: 200px; /* 左侧对齐到侧边栏的宽度 */
  transition: left 0.3s ease;
  width: calc(100% - 200px); /* 右侧内容区域的宽度 */
  overflow-y: auto; /* 允许垂直滚动 */
}

.main-content.collapsed {
  left: 50px; /* 收缩后的侧边栏宽度 */
  width: calc(100% - 50px); /* 更新收缩后的宽度 */
}

/* 内容区域样式 */
.content {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: flex-start;

  align-items: center;
  overflow-y: auto; /* 确保内容区域可滚动 */
}
</style>
