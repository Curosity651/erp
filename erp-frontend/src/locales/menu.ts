import type { Router } from 'vue-router'

type Translate = (key: string) => string
type HasTranslation = (key: string) => boolean

const camelSegment = (segment: string) =>
  segment.replace(/-([a-z0-9])/g, (_, character: string) => character.toUpperCase())

export const menuLocaleKey = (path: string) => {
  const segments = path
    .split('/')
    .filter(Boolean)
    .filter(segment => !segment.startsWith(':'))
    .map(camelSegment)
  return `menu.${segments.join('.')}`
}

export const resolveMenuLocaleKey = (path: string, hasTranslation: HasTranslation) => {
  const key = menuLocaleKey(path)
  const titleKey = `${key}._title`
  if (hasTranslation(titleKey)) return titleKey
  return hasTranslation(key) ? key : undefined
}

export const resolveMenuTitle = (
  path: string,
  fallback: string,
  translate: Translate,
  hasTranslation: HasTranslation = () => true
) => {
  const key = resolveMenuLocaleKey(path, hasTranslation)
  if (!key) return fallback
  const translated = translate(key)
  return !translated || translated === key || translated === '[object Object]' ? fallback : translated
}

export const localizeRouterTitles = (
  router: Router,
  translate: Translate,
  hasTranslation: (key: string) => boolean
) => {
  router.getRoutes().forEach(route => {
    const fallback = route.meta.originalName || route.meta.name
    if (!fallback) return
    const key = resolveMenuLocaleKey(route.path, hasTranslation)
    route.meta.locale = key || false
    route.meta.name = resolveMenuTitle(route.path, fallback, translate, hasTranslation)
  })
}
