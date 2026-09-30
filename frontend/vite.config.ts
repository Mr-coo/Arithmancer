import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    proxy: {
      // Forward API calls and the game WebSocket to the game on the VPS. changeOrigin sends its domain as the Host, so
      // the VPS's Caddy knows which site is asked for.
      '/api': { target: 'https://arithmancer.marcolinardi.site', changeOrigin: true },
      '/ws': { target: 'wss://arithmancer.marcolinardi.site', ws: true, changeOrigin: true },
    },
  },
})
