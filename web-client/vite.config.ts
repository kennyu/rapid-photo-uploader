import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    port: 3000,
    proxy: {
      '/api': {
        target: 'http://ec2-54-152-45-144.compute-1.amazonaws.com:8080',
        changeOrigin: true,
      },
    },
  },
})

