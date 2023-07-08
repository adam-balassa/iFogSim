import { ref } from "vue";
import { AggregateExperimentDetails, ExperimentDetails } from "@/types/types";
import useFetchExperimentDetails from "./use-fetch-experiment-details";
import { aggregateExperiment } from "@/views/aggregate-experiment";
import { debounce } from "lodash";

const experimentDetails = new Map<string, Promise<ExperimentDetails>>()
const selectedExperiment = ref<ExperimentDetails | AggregateExperimentDetails | null>(null)

export function experimentId(experiment: {app: string, experiment: string} | undefined) {
  return (experiment && `${experiment.app}/${experiment.experiment}`) ?? ''
}

export function idToExperiment(experimentId: string): {app: string, experiment: string} {
  const [app, experiment] = experimentId.split('/')
  return { app, experiment }
}

export default function useSelectedExperiment() {
  const { fetch } = useFetchExperimentDetails()

  async function loadExperiment(id: { app: string, experiment: string }): Promise<ExperimentDetails> {
    const experiment = experimentDetails.get(experimentId(id))
    if (experiment) {
      return experiment
    } else {
      const fetchExperiment = fetch(id.app, id.experiment)
      experimentDetails.set(experimentId(id), fetchExperiment)
      return fetchExperiment
    }
  }

  const setSelectedExperiment = debounce((value: ExperimentDetails | AggregateExperimentDetails | null) => {
    selectedExperiment.value = value
  }, 500)

  async function selectExperiment(selection: { app: string, experiment: string }[], noDebounce: boolean) {
    const experiments = await Promise.all(selection.map(loadExperiment))
    if (experiments.length === 0) {
      return
    }
    if (experiments.length === 1) {
      setSelectedExperiment(experiments[0])
    } else {
      setSelectedExperiment(aggregateExperiment(experiments))
    }
    if (noDebounce) {
      setSelectedExperiment.flush()
    }
  }


  return {
    selectedExperiment,
    selectExperiment
  }
}