import { computed, ref } from "vue";
import { ExperimentDetails } from "../types/types";
import useFetchExperimentDetails from "./use-fetch-experiment-details";

const experimentDetails = new Map<string, ExperimentDetails>()
const selectedExperimentId = ref<{ app: string, experiment: string }>()
const selectedExperiment = computed(() => experimentDetails.get(experimentId(selectedExperimentId.value)))

function experimentId(experiment: {app: string, experiment: string} | undefined) {
  return (experiment && `${experiment.app}/${experiment.experiment}`) ?? ''
}

export default function useSelectedExperiment() {
  const { fetch } = useFetchExperimentDetails()

  function selectExperiment(app: string, experiment: string) {
    const id = experimentId({ app, experiment });
    if (experimentDetails.has(id)) {
      selectedExperimentId.value = { app, experiment };
    } else {
      fetch(app, experiment).then(result => {
        experimentDetails.set(experimentId({ app, experiment }), result)
        selectedExperimentId.value = { app, experiment };
      })
    }
  }


  return {
    selectedExperimentId,
    selectedExperiment,
    selectExperiment
  }
}