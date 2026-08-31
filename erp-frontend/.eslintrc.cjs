/* eslint-env node */
require('@rushstack/eslint-patch/modern-module-resolution')

module.exports = {
  root: true,
  ignorePatterns: ['dist/**'],
  extends: [
    'eslint:recommended',
    'plugin:vue/vue3-recommended',
    '@vue/eslint-config-typescript',
    '@vue/eslint-config-prettier',
    './.eslintrc-auto-import.json'
  ],
  env: {
    node: true,
    'vue/setup-compiler-macros': true
  },
  rules: {
    // Route and reusable component entry files intentionally use index.vue/index.tsx.
    'vue/multi-word-component-names': 'off',
    // 允许使用 any
    '@typescript-eslint/no-explicit-any': 'off',
    // 允许使用 @ts-ignore 注释
    '@typescript-eslint/ban-ts-comment': 'off',
    // 允许空方法
    '@typescript-eslint/no-empty-function': 'off',
    // 允许非空断言
    '@typescript-eslint/no-non-null-assertion': 'off'
  }
}
