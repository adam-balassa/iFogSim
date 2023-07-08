export type Config = {[key: string]: string | number | boolean}

export interface ExperimentSetup {
  actuators: Config[]
  application: {
    modules: (Config & { name: string })[],
    edges: (Config & { from: string, to: string, tuple: string })[],
  }
  config: Config,
  fogDevices: (Config & {
    level: 'Cloud' | 'Proxy' | 'Gateway' | 'User',
    type?: string,
  })[]
  network: {
    id: number;
    parent: number
    level: number | string;
    group?: string;
    name: string;
    cluster?: number;
    location?: {
      lat: number;
      lng: number;
    }
  }[]
  sensors: (Config & { tuple: string })[];
  tupleTypeToCpuLength?: { [tupleType: string]: number[] }
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
  }[];
  executionLevels?: {
    [tupleType: string]: string[] | [string, number][]
  };
  waitingTuples?: {
    byTupleType: { [tupleType: string]: number },
    byDeviceId: { [deviceId: string]: number },
    byLevel: { [level: string]: number },
    byDirection: { [direction: string]: number },
  }
}

export interface AggregateExperimentResults {
  executionTime: number[];
  networkUsage: number[];
  migrationDelay: number[];
  appLoopLatencies: {
    appLoop: string[];
    avgLatency: number[];
    latencies:  number[][];
  }[];
  tupleExecutionLatencies: {
    tuple: string;
    cpuTime: number[]
  }[];
  executionLevels?: {
    [tupleType: string]: (string | [string, number])[]
  };
  waitingTuples?: {
    byTupleType: { [tupleType: string]: number },
    byDeviceId: { [deviceId: string]: number },
    byLevel: { [level: string]: number },
    byDirection: { [direction: string]: number },
  }
}

export interface ExperimentDetails {
  type: 'single';
  app: string;
  experiment: string;
  setup: ExperimentSetup;
  results: ExperimentResults;
}

export interface AggregateExperimentDetails {
  type: 'aggregate';
  app: string;
  experiment: string;
  setup: ExperimentSetup;
  results: AggregateExperimentResults;
}

export type ExperimentListing = {
  app: string;
  experiments: string[];
}[]