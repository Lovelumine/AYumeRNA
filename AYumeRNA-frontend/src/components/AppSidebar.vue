<template>
  <aside :class="['sidebar', { collapsed: props.isCollapsed }, 'd-flex flex-column']">
    <div class="header d-flex justify-content-between align-items-center">
      <h1 v-if="!props.isCollapsed" class="title">AYumeRNA</h1>
      <button class="toggle-btn btn btn-link d-flex align-items-center" @click="toggleSidebar">
        <font-awesome-icon :icon="props.isCollapsed ? 'bars' : 'times'" class="icon me-2" />
        <span v-if="!props.isCollapsed"></span> <!-- 文字和图标分开对齐 -->
      </button>
    </div>
    <p v-if="!props.isCollapsed" class="description">Generate the tRNA sequences you need</p>
    <nav v-if="!props.isCollapsed">
      <ul class="nav flex-column">
        <li v-for="route in filteredRoutes" :key="route.path" class="nav-item">
          <router-link
            v-if="!route.children || !route.children.length"
            :to="route.path"
            class="nav-link"
          >
            {{ route.meta?.title || route.name }}
          </router-link>
          <!-- 处理有子路由的情况 -->
          <div v-else>
            <button class="btn btn-link no-underline" @click="toggleSubMenu(route.path)">
              {{ isSubMenuOpen(route.path) ? '▼' : '▶' }} {{ route.meta?.title || route.name }}
            </button>
            <ul v-if="isSubMenuOpen(route.path)" class="nav flex-column ms-3">
              <li v-for="child in route.children" :key="child.path" class="nav-item">
                <router-link :to="`${route.path}/${child.path}`" class="nav-link">
                  {{ child.meta?.title || child.name }}
                </router-link>
              </li>
            </ul>
          </div>
        </li>
      </ul>
    </nav>
  </aside>
</template>

<script setup lang="ts">
import { computed, defineProps, defineEmits, ref } from 'vue';
import { useRouter } from 'vue-router';
import { FontAwesomeIcon } from '@fortawesome/vue-fontawesome';
import { faBars, faTimes } from '@fortawesome/free-solid-svg-icons';

import { library } from '@fortawesome/fontawesome-svg-core';
library.add(faBars, faTimes);

const props = defineProps<{ isCollapsed: boolean }>();
const emit = defineEmits(['toggle']);
const router = useRouter();

const subMenuOpen = ref<{ [key: string]: boolean }>({});

function toggleSidebar() {
  emit('toggle');
}

function toggleSubMenu(routePath: string) {
  subMenuOpen.value[routePath] = !subMenuOpen.value[routePath];
}

function isSubMenuOpen(routePath: string) {
  return !!subMenuOpen.value[routePath];
}

const filteredRoutes = computed(() =>
  router.options.routes.filter(route => !route.meta?.hideInSidebar)
);
</script>

<style scoped>
.sidebar {
  background-color: #2c3e50;
  color: white;
  padding: 1em;
  height: 100vh;
  transition: width 0.3s ease;

}

.sidebar.collapsed {
  width: 80px;
}

.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.title {
  font-size: 1.2em;
  font-weight: bold;
}

.toggle-btn {
  background: none;
  border: none;
  color: white;
  font-size: 1.5em;
  cursor: pointer;
  display: flex;
  align-items: center;
}

.toggle-btn .icon {
  transition: transform 0.3s ease;
}

.toggle-btn:hover .icon {
  transform: rotate(90deg);
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

nav ul li button {
  background: none;
  border: none;
  color: white;
  cursor: pointer;
  text-align: left;
  font-size: 1em;
  padding: 0.5em 1em;
  display: block;
  width: 100%;
  border-radius: 4px;
  transition: background-color 0.3s ease;
}

nav ul li button:hover {
  background-color: #3a4a5a;
}

nav ul li button.no-underline {
  text-decoration: none; /* 移除下划线 */
}

nav ul li a {
  color: white;
  text-decoration: none;
  padding: 0.5em 1em;
  display: block;
  border-radius: 4px;
  transition: background-color 0.3s ease, color 0.3s ease;
}

nav ul li a.router-link-active {
  background-color: #1a252f;
  font-weight: bold;
}

nav ul li a:hover {
  background-color: #3a4a5a;
}

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
</style>
