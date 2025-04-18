import config from '@antfu/eslint-config'

export default config({
  stylistic: {
    indent: 2,
    semi: false,
  },
  typescript: true,
  gitignore: false,
  ignores: ['dist', '.idea', '.vscode'],
  vue: false,
})
