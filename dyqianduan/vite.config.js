import { fileURLToPath, URL } from 'node:url'

import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import vueDevTools from 'vite-plugin-vue-devtools'

export default defineConfig({
  plugins: [
    vue(),
    vueDevTools(),
  ],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
  server: {
    host: true, // 允许手机通过局域网 IP 访问（配合真机测试实时聊天）
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        timeout: 120000,
        ws: true,
      },
      '/uploads': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        timeout: 120000,
      },
      // 媒体资源直连 MinIO，不经后端。生产环境由 nginx 承担同样的转发。
      '/media': {
        target: 'http://localhost:9000',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/media/, '/dydemo'),
      },
    },
  },
})
