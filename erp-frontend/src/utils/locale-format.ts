import { DEFAULT_LOCALE, type SupportedLocale } from '../locales/locale-contract'

export const formatLocaleNumber = (
  value: number,
  locale: SupportedLocale = DEFAULT_LOCALE,
  options?: Intl.NumberFormatOptions
) => new Intl.NumberFormat(locale, options).format(Number(value || 0))

export const formatLocaleCurrency = (
  value: number,
  currency: string,
  locale: SupportedLocale = DEFAULT_LOCALE,
  options?: Intl.NumberFormatOptions
) =>
  new Intl.NumberFormat(locale, {
    style: 'currency',
    currency,
    ...options
  }).format(Number(value || 0))

export const formatLocaleDate = (
  value: string | number | Date,
  locale: SupportedLocale = DEFAULT_LOCALE,
  options: Intl.DateTimeFormatOptions = { year: 'numeric', month: '2-digit', day: '2-digit' }
) => new Intl.DateTimeFormat(locale, options).format(new Date(value))

export const formatLocaleDateTime = (
  value: string | number | Date,
  locale: SupportedLocale = DEFAULT_LOCALE,
  options: Intl.DateTimeFormatOptions = {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
    hour12: false
  }
) => new Intl.DateTimeFormat(locale, options).format(new Date(value))
