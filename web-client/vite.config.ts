import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// https://vite.dev/config/
export default defineConfig(({ command, mode }) => {
  const isDev = command === 'serve' // 'serve' = dev server, 'build' = production build
  
  return {
    plugins: [react()],
    server: {
      port: 3000,
      proxy: isDev ? {
        '/api': {
          target: 'http://localhost:8080',
          changeOrigin: true,
        },
      } : undefined,
    },
    define: {
      // Fallback API URL for production builds if VITE_API_URL is not set
      'import.meta.env.VITE_API_URL': isDev 
        ? JSON.stringify('/api')
        : JSON.stringify(process.env.VITE_API_URL || 'http://ec2-54-152-45-144.compute-1.amazonaws.com:8080/api')
    },
  }
})

