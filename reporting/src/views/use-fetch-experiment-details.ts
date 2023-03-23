import { useFetch } from "@vueuse/core";
import { ExperimentDetails } from "../types/types";
export default function useFetchExperimentDetails() {

  async function fetchExperimentDetails(app: string, experiment: string): Promise<ExperimentDetails> {
    const url = `/experiment?app=${encodeURIComponent(app)}&experiment=${encodeURIComponent(experiment)}`
    const { data, error } = await useFetch(url.toString()).json<Pick<ExperimentDetails, 'setup'>>()
    if (!data.value || error.value) throw new Error('Failed to load experiment details')

    return { ...data.value, app, experiment }
  }

  return { fetch: fetchExperimentDetails }
}