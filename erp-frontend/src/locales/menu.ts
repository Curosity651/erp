import type { Router } from 'vue-router'

type Translate = (key: string) => string

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

export const resolveMenuTitle = (path: string, fallback: string, translate: Translate) => {
  const key = menuLocaleKey(path)
  const translated = translate(key)
  return translated === key ? fallback : translated
}

export const localizeRouterTitles = (router: Router, translate: Translate) => {
  router.getRoutes().forEach(route => {
    const fallback = route.meta.originalName || route.meta.name
    if (fallback) route.meta.name = resolveMenuTitle(route.path, fallback, translate)
  })
}
