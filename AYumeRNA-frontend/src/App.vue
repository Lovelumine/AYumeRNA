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
  min-height: 100vh;
}

/* 动态调整右侧内容区域的左边距 */
.main-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  margin-left: 200px; /* 默认的侧边栏宽度 */
  transition: margin-left 0.3s ease;
}

.main-content.collapsed {
  margin-left: 50px; /* 收缩后的侧边栏宽度 */
}

/* 内容区域样式 */
.content {
  flex: 1;
  padding: 2em;
}
</style>
