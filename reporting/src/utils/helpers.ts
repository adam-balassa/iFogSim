import { AggregateExperimentResults, Config, ExperimentResults } from "@/types/types";
import { ref, Ref, watch } from "vue";
import { isEqual } from "lodash";

export function useComputedRef<T, R>(source: Ref<R>, getter: (r: R) => T) {
  const computedRef = ref<T>(getter(source.value))
  watch(source, (nextValue, prevValue) => {
    if (!isEqual(nextValue, prevValue)){
      computedRef.value = ref(getter(nextValue)).value
    }
  })
  return computedRef
}

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

type TemporalExecutionLevels = { [tupleType: string]: [string, number][] }
export function areTemporalExecutionLevels(executionLevels: AggregateExperimentResults['executionLevels']): executionLevels is TemporalExecutionLevels {
  if (executionLevels == null) return false
  return Object.values(executionLevels)[0].every(measurement => Array.isArray(measurement))
}

type ExecutionLevels = { [tupleType: string]: string[] }
export function areSimpleExecutionLevels(executionLevels: AggregateExperimentResults['executionLevels']): executionLevels is ExecutionLevels {
  if (executionLevels == null) return false
  return Object.values(executionLevels)[0].every(measurement => !Array.isArray(measurement))
}