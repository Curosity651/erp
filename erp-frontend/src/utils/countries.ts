import countriesData from '@/data/countries.json'

export interface CountryOption {
  code: string
  name: string
  englishName: string
  flag: string
}

// 导出国家数据
export const countries: CountryOption[] = countriesData

// 根据国家代码获取国家信息
export const getCountryByCode = (code: string): CountryOption | undefined => {
  return countries.find(country => country.code === code)
}

// 获取国家显示名称
export const getCountryDisplayName = (code: string): string => {
  const country = getCountryByCode(code)
  return country ? `${country.flag} ${country.name} (${country.englishName})` : code
}

// 搜索国家
export const searchCountries = (query: string): CountryOption[] => {
  const searchText = query.toLowerCase()
  return countries.filter(
    country =>
      country.name.toLowerCase().includes(searchText) ||
      country.englishName.toLowerCase().includes(searchText) ||
      country.code.toLowerCase().includes(searchText)
  )
}
