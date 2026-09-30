import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    proxy: {
      // Forward API calls and the game WebSocket to the backend on the VPS
      '/api': 'http://43.156.104.167',
      '/ws': { target: 'ws://43.156.104.167', ws: true },
    },
  },
})
