export type UtilizationLevel = 'low' | 'medium' | 'high'

export function calculateUtilization(used?: number, capacity?: number): number {
  if (!capacity || capacity <= 0 || !used || used <= 0) return 0
  return Math.min(100, Math.round((used / capacity) * 1000) / 10)
}

export function utilizationLevel(percent: number): UtilizationLevel {
  if (percent >= 90) return 'high'
  if (percent >= 60) return 'medium'
  return 'low'
}

export function formatVolume(volumeMm3?: number): string {
  if (!volumeMm3 || volumeMm3 <= 0) return '0 m³'
  return `${(volumeMm3 / 1_000_000_000).toFixed(3)} m³`
}
