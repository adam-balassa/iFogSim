import { computed, ref } from "vue";
import { AggregateExperimentDetails, ExperimentDetails } from "@/types/types";
import useFetchExperimentDetails from "./use-fetch-experiment-details";
import { aggregateExperiment } from "@/views/aggregate-experiment";

const experimentDetails = new Map<string, ExperimentDetails>()
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
      return await fetch(id.app, id.experiment).then(result => {
        experimentDetails.set(experimentId(id), result)
        return result
      })
    }
  }

  async function selectExperiment(selection: { app: string, experiment: string }[]) {
    const experiments = await Promise.all(selection.map(loadExperiment))
    if (experiments.length === 0) {
      return
    }
    if (experiments.length === 1) {
      selectedExperiment.value = experiments[0]
    } else {
      selectedExperiment.value = aggregateExperiment(experiments)
    }
  }


  return {
    selectedExperiment,
    selectExperiment
  }
}