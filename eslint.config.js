// eslint.config.js — ESLint v9 flat config for react-native-nitro-video
const js = require('@eslint/js')
const prettierPlugin = require('eslint-plugin-prettier')
const prettierConfig = require('eslint-config-prettier')

module.exports = [
  // Base JS recommended rules
  js.configs.recommended,

  // Prettier formatting rules (must come last to override conflicting rules)
  prettierConfig,

  {
    plugins: {
      prettier: prettierPlugin,
    },
    rules: {
      'prettier/prettier': [
        'warn',
        {
          quoteProps: 'consistent',
          singleQuote: true,
          tabWidth: 2,
          trailingComma: 'es5',
          useTabs: false,
          semi: false,
        },
      ],
      // Allow unused variables that start with _ (common pattern for intentionally unused)
      'no-unused-vars': [
        'warn',
        { argsIgnorePattern: '^_', varsIgnorePattern: '^_' },
      ],
    },
    languageOptions: {
      ecmaVersion: 'latest',
      sourceType: 'module',
      globals: {
        __DEV__: 'readonly',
        console: 'readonly',
        require: 'readonly',
        module: 'writable',
        exports: 'writable',
        process: 'readonly',
        Promise: 'readonly',
        Array: 'readonly',
        Map: 'readonly',
        Set: 'readonly',
        JSON: 'readonly',
        URL: 'readonly',
        setTimeout: 'readonly',
        clearTimeout: 'readonly',
        setInterval: 'readonly',
        clearInterval: 'readonly',
      },
    },
    ignores: ['node_modules/**', 'lib/**', 'nitrogen/**', 'expo-video/**'],
  },
]
