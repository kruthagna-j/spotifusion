import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'
import { fileURLToPath } from 'node:url'
import path from 'node:path'

const rootDir = path.dirname(fileURLToPath(import.meta.url))

export default defineConfig({
  plugins: [
    react(),
    tailwindcss(),
  ],

  resolve: {
    alias: {
      '@': path.resolve(rootDir, 'src'),
    },
  },

  build: {
    target: 'es2022',
    sourcemap: false,
    minify: 'esbuild',
    cssMinify: 'esbuild',
    chunkSizeWarningLimit: 1000,
  },

  server: {
    host: true,
    port: 5173,
  },
})
