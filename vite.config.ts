import path from 'node:path'
import vue from '@vitejs/plugin-vue'
import { defineConfig } from 'vite'
import dts from 'vite-plugin-dts'

export default defineConfig({
  build: {
    minify: false,
    lib: {
      entry: {
        main: path.resolve(__dirname, 'src/index.ts'),
        shared: path.resolve(__dirname, 'src/shared.ts'),
      },
      name: 'airpower-core',
      formats: ['es'],
      fileName: (format, entryName) => {
        return `airpower.${entryName === 'main' ? 'core' : entryName}.js`
      },
    },
    rollupOptions: {
      external: [],
      output: {
        globals: {},
      },
    },
  },
  plugins: [dts({
    include: ['src'],
  }), vue()],
})
