<template>
  <aside :class="['sidebar', { collapsed: props.isCollapsed }]">
    <div class="header">
      <h1 v-if="!props.isCollapsed" class="title">AYumeRNA</h1>
      <button class="toggle-btn" @click="toggleSidebar">
        <font-awesome-icon :icon="props.isCollapsed ? 'bars' : 'times'" class="icon" />
      </button>
    </div>
    <p v-if="!props.isCollapsed" class="description">Generate the tRNA sequences you need</p>
    <nav v-if="!props.isCollapsed">
      <ul>
        <li v-for="route in filteredRoutes" :key="route.path">
          <router-link :to="route.path">{{ route.meta?.title || route.name }}</router-link>
          <!-- 二级路由 -->
          <ul v-if="route.children && route.children.length">
            <li v-for="child in route.children" :key="child.path">
              <router-link :to="child.path">{{ child.meta?.title || child.name }}</router-link>
            </li>
          </ul>
        </li>
      </ul>
    </nav>
  </aside>
</template>

<script setup lang="ts">
import { computed, defineProps, defineEmits } from 'vue';
import { useRouter } from 'vue-router';
import { FontAwesomeIcon } from '@fortawesome/vue-fontawesome';
import { faBars, faTimes } from '@fortawesome/free-solid-svg-icons';

// 注册 FontAwesome 图标
import { library } from '@fortawesome/fontawesome-svg-core';
library.add(faBars, faTimes);

const props = defineProps<{ isCollapsed: boolean }>();
const emit = defineEmits(['toggle']);

function toggleSidebar() {
  emit('toggle');
}

// 获取路由对象
const router = useRouter();

// 过滤出需要展示的一级和二级路由
const filteredRoutes = computed(() =>
  router.options.routes.filter(route => !route.meta?.hideInSidebar)
);
</script>

<style scoped>
.sidebar {
  width: 200px;
  background-color: #2c3e50;
  color: white;
  padding: 1em;
  display: flex;
  flex-direction: column;
  justify-content: start;
  min-height: 100vh;
  position: fixed;
  top: 0;
  left: 0;
  transition: width 0.3s ease;
  z-index: 1000;
}

.sidebar.collapsed {
  width: 50px;
}

.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.title {
  font-size: 1.2em;
  color: white;
  margin: 0;
  font-weight: bold;
}

.toggle-btn {
  background: none;
  border: none;
  color: white;
  font-size: 1.5em;
  cursor: pointer;
  transition: transform 0.3s ease;
}

.toggle-btn .icon {
  transition: transform 0.3s ease;
}

.toggle-btn:hover .icon {
  transform: rotate(90deg); /* 添加旋转效果 */
}

.description {
  color: #a9b7c6;
  font-size: 0.9em;
  margin-top: 0.5em;
  margin-bottom: 1em;
}

nav ul {
  list-style: none;
  padding: 0;
  margin: 0;
}

nav ul li {
  margin-bottom: 1em;
}

nav ul li a {
  color: white;
  text-decoration: none;
  padding: 0.5em 1em;
  display: block;
  border-radius: 4px;
  transition: background-color 0.3s ease, color 0.3s ease;
}

/* 当前页面的链接高亮效果 */
nav ul li a.router-link-active {
  background-color: #1a252f;
  font-weight: bold;
}

/* 鼠标悬停效果 */
nav ul li a:hover {
  background-color: #3a4a5a;
  color: #ffffff;
}

/* 鼠标移走时的恢复效果 */
nav ul li a:not(.router-link-active):hover {
  background-color: #2c3e50;
}

/* 二级路由样式 */
nav ul ul {
  padding-left: 1em;
}

nav ul ul li a {
  font-size: 0.9em;
  color: #a9b7c6;
}

nav ul ul li a.router-link-active {
  background-color: #1a252f;
  color: #ffffff;
  font-weight: bold;
}

/* 手机端样式 */
@media (max-width: 768px) {
  .sidebar {
    width: 100%;
    height: auto;
    position: relative;
  }

  .toggle-btn {
    position: absolute;
    top: 10px;
    left: 10px;
  }

  nav ul {
    display: flex;
    justify-content: space-around;
  }
}
</style>
