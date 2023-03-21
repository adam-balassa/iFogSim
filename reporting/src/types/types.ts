export interface ExperimentSetup {
  actuators: any[]
  application: {
    modules: any[],
    edges: any[],
  }
  config: {[key: string]: any},
  fogDevices: any[]
  network: {
    id: number;
    parent: number
    level: string;
    name: string;
  }[]
  sensors: any[];
}

export interface Experiment {
  setup: ExperimentSetup
}

export type ExperimentListing = {
  app: string;
  experiments: string[];
}[]