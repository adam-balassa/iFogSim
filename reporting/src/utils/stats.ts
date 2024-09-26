import { T } from '@stdlib/stats/base/dists/t'
import std from '@stdlib/stats/base/stdev'
import stdMean from "@stdlib/stats/base/mean";
import { zipSafe } from "@/utils/zip";
import _, { countBy, identity } from "lodash";

export function mean(sample: number[]) {
  return stdMean(sample.length, sample, 1)
}

/**
 * Calculates the confidence interval for the mean of a normally distributed sample
 * @param sample for which the confidence interval should be calculated
 * @param alpha size of the confidence interval [(1-alpha)/2, 1-(1-alpha)/2]
 * @returns `[mean, C]` where the confidence interval is `mean +- C`
 */
export function confidenceInterval(sample: number[], alpha: number = 0.9): [number, number] {
  const n = sample.length
  const t = new T(sample.length - 1)
  const quantile = 1 - (1-alpha) / 2
  return [
    stdMean(n, sample, 1),
    t.quantile(quantile) * Math.sqrt(std(n, 1, sample, 1)**2 / n)
  ]
}

export function CDF(data: number[]): { x: number, y: number }[] {
  const min = _.min(data)!
  const max = _.max(data)!
  const points = 100
  const step = Math.ceil((max - min) / points)
  data.sort((a, b) => a - b)
  const X = Array.from({length: Math.floor((max - min) / step)}, (_, i) => Math.ceil(min) + i * step)
  return X.map(x => ({ x, y: data.findIndex(el => el >= x)! }))
}

export function histogram(arr: number[], numberOfBins = 20): [number, number][] {
  arr.sort((a, b) => a - b)
  const discreteHist = discreteHistogram(arr)
  if (discreteHist.filter(([, count]) => count > 1).length > 5 && discreteHist.length < 100) {
    return discreteHist
  }

  const max = Math.max(...arr);
  const min = Math.min(...arr);
  const range = max - min + 1;
  const bins = new Array<number>(numberOfBins).fill(0);
  const binWidth = range / numberOfBins
  arr.forEach((el) => bins[Math.floor((el - min) / binWidth)]++);
  const partitions = new Array(numberOfBins).fill(0).map((_, i) => min + i * binWidth)
  return zipSafe(partitions, bins);
}

export function bins<T extends { num: number }>(arr: T[], numberOfBins = 10): [number, T[]][] {
  const max = Math.max(...arr.map(el => el.num));
  const min = Math.min(...arr.map(el => el.num));
  const range = max - min + 1;
  const bins = new Array(numberOfBins).fill(0).map<T[]>(() => [])
  const binWidth = range / numberOfBins
  arr.forEach((el) => {
    bins[Math.floor((el.num - min) / binWidth)].push(el)
  });
  const partitions = new Array(numberOfBins).fill(0).map((_, i) => min + i * binWidth)
  return zipSafe(partitions, bins);
}

function discreteHistogram(arr: number[]): [number, number][] {
  return Object.entries(countBy(arr, identity)).map(([val, count]) => [+val, count])
}

/**
 * Takes a list of CDFs and constructs a min and a max CDF
 * @param cdfs the functions to combine
 */
export function minMaxCDFs(cdfs: { x: number, y: number }[][]): [{ x: number, y: number }[], { x: number, y: number }[]] {
  const min = _.min(cdfs.flatMap(cdf => cdf.map(point => point.x)))!
  const max = _.max(cdfs.flatMap(cdf => cdf.map(point => point.x)))!
  const X = Array.from({length: Math.floor(max - min)}, (_, i) => min + i)
  return [
    X.map(x => ({ x, y: _.min(cdfs.map(cdf => cdf.find(point => point.x >= x)?.y))!})),
    X.map(x => ({ x, y: _.max(cdfs.map(cdf => cdf.find(point => point.x >= x)?.y))!})),
  ]
}