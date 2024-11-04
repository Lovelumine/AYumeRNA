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
  min-height: 100vh; /* 确保填满视口高度 */
  height: 100%;
  top: 0;
  left: 0;

}

/* 动态调整右侧内容区域的左边距 */
.main-content {
  position: fixed;
  top: 0;
  right: 0;
  bottom: 0;
  left: 200px; /* 左侧对齐到侧边栏的宽度 */
  transition: left 0.3s ease; /* 在侧边栏收缩时调整宽度 */
  display: flex;
  flex-direction: column;
}


.main-content.collapsed {
  left: 50px; /* 收缩后的侧边栏宽度 */
}


/* 内容区域样式 */
.content {
  flex: 1;

  display: flex;
  justify-content: center;
  align-items: center;
}
</style>
