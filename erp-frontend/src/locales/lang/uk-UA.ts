import antdLocale from 'ant-design-vue/es/locale/uk_UA'
import 'dayjs/locale/uk'

const messages: Record<string, unknown> = {
  antdLocale,
  'dayjs.language': 'uk',
  'locale.language': 'uk-UA'
}

const modules = import.meta.glob('./uk-UA/**/*.json', { eager: true })
for (const path in modules) {
  const json = (modules[path] as { default: Record<string, unknown> }).default
  Object.assign(messages, json)
}

export default messages
