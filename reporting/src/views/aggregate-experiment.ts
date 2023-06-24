import {
  AggregateExperimentDetails,
  AggregateExperimentResults,
  Config,
  ExperimentDetails,
  ExperimentSetup
} from "@/types/types";
import _, { uniq } from "lodash";
import { mean } from "@/utils/stats";
import { zipSafe } from "@/utils/zip";

export function aggregateExperiment(experiments: ExperimentDetails[]): AggregateExperimentDetails | null {
  const setup: ExperimentSetup = {
    actuators: aggregateConfigs(experiments.map(e => e.setup.actuators)),
    application: {
      modules: aggregateConfigs(experiments.map(e => e.setup.application.modules)),
      edges: aggregateConfigs(experiments.map(e => e.setup.application.edges)),
    },
    config: aggregateConfig(experiments.map(e => e.setup.config)),
    fogDevices: aggregateConfigs(experiments.map(e => e.setup.fogDevices)),
    network: aggregateConfigs(experiments.map(e => e.setup.network.map(device => {
      const { location, ...networkSettings } = device
      return networkSettings
    }))),
    sensors: aggregateConfigs(experiments.map(e => e.setup.sensors)),
    tupleTypeToCpuLength: _(experiments.map(e => e.setup.tupleTypeToCpuLength))
      .reduce((acc, next) => {
        next && Object.entries(next).forEach(([tupleType, cpuLengths]) => {
          acc[tupleType] = [...(acc[tupleType] ?? []), ...cpuLengths]
        })
        return acc
      }, {} as { [tupleType: string]: number[] })
  }
  const results: AggregateExperimentResults = {
    executionTime: experiments.map(e => e.results.executionTime),
    networkUsage: experiments.map(e => e.results.networkUsage),
    migrationDelay: experiments.map(e => e.results.migrationDelay),
    appLoopLatencies: zipSafe(...experiments.map(e => e.results.appLoopLatencies))
      .map<AggregateExperimentResults['appLoopLatencies'][number]>(latencies => ({
        appLoop: latencies[0].appLoop,
        avgLatency: latencies.map(l => l.avgLatency),
        latencies:  latencies.map(l => l.latencies),
      })
    ),
    tupleExecutionLatencies: _(experiments.flatMap(e => e.results.tupleExecutionLatencies))
      .groupBy(t => t.tuple)
      .map((measurements, tuple) => ({
        tuple,
        cpuTime: measurements.map(m => m.cpuTime)
      }))
      .value(),
    executionLevels: _(experiments.map(e => e.results.executionLevels))
      .reduce((acc, next) => {
        next && Object.entries(next).forEach(([tupleType, levels]) => {
          acc[tupleType] = [...(acc[tupleType] ?? []), ...levels]
        })
        return acc
      }, {} as { [tupleType: string]: (string | [string, number])[] })
  }
  return {
    type: 'aggregate',
    setup,
    results,
    app: aggregateConfigValue(experiments.map(e => e.app)),
    experiment: aggregateConfigValue(experiments.map(e => e.experiment)),
  }
}

function aggregateConfigs<T extends Config>(configs: T[][]): T[] {
  return zipSafe(...configs).map(aggregateConfig) as T[]
}

function aggregateConfig(configs: Config[]): Config {
  const aggregatedEntries = _(configs)
    .flatMap(c => Object.entries(c))
    .groupBy(([key]) => key)
    .mapValues((values) => values.map(([,value]) => value))
    .mapValues((values) => aggregateConfigValue(values))
    .entries()
    .value()

  return Object.fromEntries(aggregatedEntries)
}

function aggregateConfigValue<T extends Config[keyof Config]>(value: T[]): T {
  if (typeof value[0] === 'boolean' || typeof value[0] === 'string') {
    return uniq(value).join(', ') as T
  }
  return mean(value as number[]) as T
}