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

export interface ExperimentDetails {
  app: string;
  experiment: string;
  setup: ExperimentSetup
}

export type ExperimentListing = {
  app: string;
  experiments: string[];
}[]