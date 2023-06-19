import { zip } from 'lodash';

export function zipSafe<T1, T2>(arr1: T1[], arr2: T2[]): [T1, T2][];
export function zipSafe<T1, T2, T3>(arr1: T1[], arr2: T2[], arr3: T3[]): [T1, T2, T3][];
export function zipSafe<T>(...arrays: T[][]): T[][];
export function zipSafe<T>(...arrays: T[][]): T[][] {
  if (!arrays.every((arr) => arr.length === arrays[0].length)) {
    console.trace('Different length arrays zipped');
  }
  return zip(...arrays).filter(allDefined);
}

export function allDefined<T>(zipped: (T | undefined)[]): zipped is T[] {
  return zipped.every((value) => value != null);
}
