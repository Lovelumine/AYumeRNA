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
  server: {
    proxy: {
      '/sample': {
        target: 'http://127.0.0.1:36243', // 转发请求到本地的 127.0.0.1
        changeOrigin: true, // 修改请求头中的 Origin 字段
      },
      '/info': {
        target: 'http://127.0.0.1:36243', // 转发请求到本地的 127.0.0.1
        changeOrigin: true, // 修改请求头中的 Origin 字段
      },
      '/ws': {
        target: 'http://127.0.0.1:36243', // 转发请求到本地的 127.0.0.1
        ws: true, // 启用 WebSocket 代理
        changeOrigin: true, // 修改请求头中的 Origin 字段
      },
      '/topic/progress': {
        target: 'http://127.0.0.1:36243', // 后端 WebSocket 服务地址
        ws: true, // 启用 WebSocket 代理
        changeOrigin: true, // 修改请求头中的 Origin 字段
      },
      '/sockjs/ws': {
        target: 'http://127.0.0.1:36243', // 后端 WebSocket 服务地址
        ws: true, // 启用 WebSocket 代理
        changeOrigin: true, // 修改请求头中的 Origin 字段
      },
      '/sequence/process': {
        target: 'http://127.0.0.1:36243', // 后端 WebSocket 服务地址
        ws: true, // 启用 WebSocket 代理
        changeOrigin: true, // 修改请求头中的 Origin 字段
      },
    },
  },
  define: {
    // 在 Vite 中配置 global 对象
    global: 'window', // 全局变量设置为 window 对象
  },
  optimizeDeps: {
    // 解决一些依赖可能使用 Node.js 内置模块的问题
    include: [
      'sockjs-client', // 你可以添加你遇到问题的依赖
    ],
  },
})
