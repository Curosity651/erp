import fs from 'node:fs'
import path from 'node:path'

const locales = ['zh-CN', 'en-US', 'uk-UA', 'ru-RU']
const root = path.resolve('src/locales/lang')

const flatten = (value, prefix = '', result = new Map()) => {
  for (const [key, child] of Object.entries(value)) {
    const fullKey = prefix ? `${prefix}.${key}` : key
    if (child && typeof child === 'object' && !Array.isArray(child)) flatten(child, fullKey, result)
    else result.set(fullKey, child)
  }
  return result
}

const readLocale = locale => {
  const directory = path.join(root, locale)
  const files = fs.readdirSync(directory).filter(file => file.endsWith('.json')).sort()
  const merged = {}
  for (const file of files) Object.assign(merged, JSON.parse(fs.readFileSync(path.join(directory, file), 'utf8')))
  return flatten(merged)
}

const resources = Object.fromEntries(locales.map(locale => [locale, readLocale(locale)]))
const expected = [...resources['zh-CN'].keys()].sort()
const failures = []
for (const locale of locales) {
  const keys = [...resources[locale].keys()].sort()
  const missing = expected.filter(key => !resources[locale].has(key))
  const extra = keys.filter(key => !resources['zh-CN'].has(key))
  const empty = keys.filter(key => String(resources[locale].get(key) ?? '').trim() === '')
  if (missing.length || extra.length || empty.length) failures.push({ locale, missing, extra, empty })
}
if (failures.length) {
  console.error(JSON.stringify(failures, null, 2))
  process.exit(1)
}
console.log(`i18n resources are aligned: ${expected.length} keys in ${locales.length} locales`)
