import { shallowRef, watch } from "vue";
import { useFetch } from "@vueuse/core";
import { ExperimentListing } from "../../types/types";

const experimentListing = shallowRef<ExperimentListing>()

export default function useListExperiments() {

  async function fetchExperiments() {
    const { data, error } = await useFetch('/experiments').json<ExperimentListing>()
    if (!data.value || error.value) throw new Error('Failed to load experiments')
    experimentListing.value = data.value
  }

  if (!experimentListing.value) {
    fetchExperiments()
  }

  return {
    experimentListing
  }
}