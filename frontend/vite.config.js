// vite.config.js
import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig({
  plugins: [react()],
  /*
  server: {
    proxy: {
      // Comment out this section temporarily
    }
  }
  */
})