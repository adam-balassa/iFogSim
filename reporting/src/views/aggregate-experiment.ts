import {
  AggregateExperimentDetails,
  AggregateExperimentResults,
  Config,
  ExperimentDetails,
  ExperimentSetup
} from "@/types/types";
import { uniq } from "lodash";
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
  }
  console.log(setup, results)
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
  return zipSafe(...configs.map(c => Object.entries(c)))
    .reduce<Config>((config, zipped) => {
      return { ...config, [zipped[0][0]]: aggregateConfigValue(zipped.map(([, values]) => values)) }
    }, {})
}

function aggregateConfigValue<T extends Config[keyof Config]>(value: T[]): T {
  if (typeof value[0] === 'boolean' || typeof value[0] === 'string') {
    return uniq(value).join(', ') as T
  }
  return mean(value as number[]) as T
}