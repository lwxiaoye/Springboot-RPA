import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import path from 'path'

// https://vite.dev/config/
export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, './src')
    }
  },
  define: {
    global: 'globalThis'
  },
  server: {
    port: 5173,
    host: '0.0.0.0',
    strictPort: true,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        secure: false
      },
      '/ws': {
        target: 'http://localhost:8080',
        ws: true
      },
      '/ws/chat': {
        target: 'http://localhost:8080',
        ws: true,
        rewrite: (path) => path
      }
    }
  },
  optimizeDeps: {
    include: ['element-plus', 'vue', 'vue-router', 'echarts', 'vue-echarts', 'sockjs-client', 'stompjs']
  }
})
