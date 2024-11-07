import { fileURLToPath, URL } from 'node:url'

import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import vueJsx from '@vitejs/plugin-vue-jsx'

// https://vite.dev/config/
export default defineConfig({
  plugins: [
    vue(), // 启用 Vue 插件
    vueJsx(), // 启用 JSX 插件
  ],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)), // 设置路径别名
    },
  },
  esbuild: {
    jsxFactory: 'h', // JSX 工厂方法
    jsxFragment: 'Fragment', // JSX Fragment 支持
  },
})
