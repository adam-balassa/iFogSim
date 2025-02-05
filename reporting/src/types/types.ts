export type Config = {[key: string]: string | number | boolean}

export interface ExperimentSetup {
  actuators: Config[]
  application: {
    modules: (Config & { name: string })[],
    edges: (Config & { from: string, to: string, tuple: string })[],
  }
  config: Config,
  fogDevices: (Config & { level: 'Cloud' | 'Proxy' | 'Gateway' | 'User' })[]
  network: {
    id: number;
    parent: number
    level: number | string;
    group?: string;
    name: string;
  }[]
  sensors: (Config & { tuple: string })[];
}

export interface ExperimentResults {
  executionTime: number;
  networkUsage: number;
  migrationDelay: number;
  appLoopLatencies: {
    appLoop: string[];
    avgLatency: number;
    latencies:  number[];
  }[];
  tupleExecutionLatencies: {
    tuple: string;
    cpuTime: number
  }[];
  fogDeviceEnergyConsumptions: {
    group: string;
    name: string;
    energy: number;
  }[]
}

export interface ExperimentDetails {
  app: string;
  experiment: string;
  setup: ExperimentSetup;
  results: ExperimentResults;
}

export type ExperimentListing = {
  app: string;
  experiments: string[];
}[]