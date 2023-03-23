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