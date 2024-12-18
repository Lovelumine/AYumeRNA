import router from "@/router";
import type { SequenceData } from "./tableConfig";

// 氨基酸与 ΔΔG° 数据
export const aminoAcids = [
  { name: 'Glu', deltaG: 2.8 },
  { name: 'Asp', deltaG: 1.9 },
  { name: 'Ala', deltaG: 1.4 },
  { name: 'Gly', deltaG: 0.7 },
  { name: 'Gln', deltaG: -1.4 },
  { name: 'Trp', deltaG: -1.1 }
]

// T-stem 碱基对 ΔΔG° 数据表
export const positionBasedDeltaG: Record<string, Record<string, number>> = {
  "49-65": { CG: 0.0, UA: -0.2, GC: -0.4, AU: -0.5, GU: -0.9 },
  "50-64": { GU: 1.4, UG: 0.4, GC: 0.0, AU: 0.0, UA: 0.0, CG: -0.2 },
  "51-63": { AU: 0.1, UA: 0.0, UG: 0.0, GU: -0.2, CG: -0.5, AC: -0.8, GC: -1.0 }
}

// 计算自由能
export function calculateFreeEnergy(tstemSequence: string): { basePairs: string[]; energy: number } {
  const positions = Object.keys(positionBasedDeltaG)
  const pairs: string[] = []
  let energy = 0

  for (let i = 0; i < positions.length; i++) {
    const pair = `${tstemSequence[i]}${tstemSequence[tstemSequence.length - 1 - i]}`
    pairs.push(pair)
    energy += positionBasedDeltaG[positions[i]][pair] ?? 0
  }

  return { basePairs: pairs, energy: energy }
}

// 存储序列并跳转到 /visualization
export function handleAnalyzeSequence(record: SequenceData) {
  localStorage.setItem('analyzedSequence', JSON.stringify(record))
  router.push('/visualization').then(() => {
    console.log('Navigated to /visualization')
  })
}
