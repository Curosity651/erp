import antdLocale from 'ant-design-vue/es/locale/ru_RU'
import 'dayjs/locale/ru'

const messages: Record<string, unknown> = {
  antdLocale,
  'dayjs.language': 'ru',
  'locale.language': 'ru-RU'
}

const modules = import.meta.glob('./ru-RU/**/*.json', { eager: true })
for (const path in modules) {
  const json = (modules[path] as { default: Record<string, unknown> }).default
  Object.assign(messages, json)
}

export default messages
