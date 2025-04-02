import path from 'node:path'
import { defineConfig } from 'vite'
import dts from 'vite-plugin-dts'

export default defineConfig({
  build: {
    minify: false,
    lib: {
      entry: path.resolve(__dirname, 'src/index.ts'),
      name: 'airpower-core',
      formats: ['es'],
      fileName: () => `airpower.core.js`,
    },
    rollupOptions: {
      external: ['airpower'],
    },
  },
  plugins: [dts()],
})
