import dayjs from 'dayjs'

export function formatAccountTime(value?: string) {
  if (!value) return '--'
  const time = dayjs(value)
  if (!time.isValid() || time.year() <= 1970) return '--'
  return time.format('YYYY-MM-DD HH:mm:ss')
}
