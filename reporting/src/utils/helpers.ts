import { Config } from "../types/types";

export function transposeConfigs<T extends Config>(configs: T[], columnName?: keyof T): (Config & { metric: string })[] {
  return Object.entries(configs[0])
      .filter(([field]) => field !== columnName )
      .map(([field]) => ({
        metric: field,
        ...Object.fromEntries(configs.map(config => [
          (columnName && config[columnName]) ?? 'value', config[field]
        ]))
      }))
}

export function CDF(data: number[]): { x: number, y: number }[] {
  const acc: { x: number, y: number }[] = []
  data.sort((a, b) => a - b).reduce<number>((currentCount, nextValue) => {
    acc.push({ x: nextValue, y: (currentCount + 1) })
    return currentCount + 1;
  }, 0)
  return acc
}