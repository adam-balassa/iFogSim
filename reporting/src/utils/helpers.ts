import { Config } from "@/types/types";
import { ref, Ref, watch } from "vue";

export function useComputedRef<T, R>(source: Ref<R>, getter: (r: R) => T) {
  const computedRef = ref<T>(getter(source.value))
  watch(source, nextValue => {
    computedRef.value = ref(getter(nextValue)).value
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

