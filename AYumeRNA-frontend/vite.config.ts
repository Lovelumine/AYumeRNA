import { fileURLToPath, URL } from 'node:url'
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import vueJsx from '@vitejs/plugin-vue-jsx'

// https://vite.dev/config/
export default defineConfig({
  plugins: [vue(), vueJsx()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
  esbuild: {
    jsxFactory: 'h',
    jsxFragment: 'Fragment',
  },
  server: {
    proxy: {
      '/sample': {
        target: 'http://223.82.75.77:36243',
        changeOrigin: true,
      },
      '/info': {
        target: 'http://223.82.75.77:36243',
        changeOrigin: true,
      },
      '/ws': {
        target: 'http://223.82.75.77:36243',
        ws: true,
        changeOrigin: true,
      },
      '/topic/progress': {
        target: 'http://223.82.75.77:36243',
        ws: true,
        changeOrigin: true,
      },
      '/sockjs/ws': {
        target: 'http://223.82.75.77:36243',
        ws: true,
        changeOrigin: true,
      },
      '/sequence/process': {
        target: 'http://223.82.75.77:36243',
        changeOrigin: true,
      },
      '/r2dt/run': {
        target: 'http://127.0.0.1:2002',
        changeOrigin: true,
      },
      '/scape/analyze':{
        target: 'http://127.0.0.1:2002',
        changeOrigin: true,
      },
      // 新增反向代理规则，将 /ayumerna 转发到 https://minio.lumoxuan.cn
      '/ayumerna': {
        target: 'https://minio.lumoxuan.cn',
        changeOrigin: true,
        rewrite: path => path.replace(/^\/ayumerna/, '/ayumerna'),
      },
    },
  },
  define: {
    global: 'window',
  },
  optimizeDeps: {
    include: ['sockjs-client'],
  },
})
