<template>
  <div class="free-energy-evaluation">
    <h3>Thermodynamic Free Energy Evaluation</h3>
    <p>
      Select an amino acid and specific T-stem base pairs to calculate the total
      ΔG° value for tRNA binding with EF-Tu.
    </p>

    <!-- 氨基酸选择框 -->
    <div class="amino-acid-selection">
      <label for="amino-acid">Select Amino Acid:</label>
      <select v-model="selectedAminoAcid" id="amino-acid">
        <option
          v-for="aa in aminoAcids"
          :key="aa.name"
          :value="aa.deltaG"
        >
          {{ aa.name }} (ΔΔG°: {{ aa.deltaG }})
        </option>
      </select>
    </div>

    <!-- 三个T-stem碱基对选择框 -->
    <div class="tstem-selection" v-for="region in tStemRegions" :key="region.region">
      <label :for="region.region">Select Base Pair ({{ region.region }}):</label>
      <select v-model="selectedPairs[region.region]" :id="region.region">
        <option
          v-for="pair in region.pairs"
          :key="pair.position"
          :value="pair.deltaG"
        >
          {{ pair.position }} (ΔΔG°: {{ pair.deltaG }})
        </option>
      </select>
    </div>

    <!-- 计算并显示总的 ΔG° -->
    <div class="result">
      <p>
        Total ΔG° (tRNA): <strong>{{ calculateTotalDeltaG() ?? 'Not Calculated' }} kcal/mol</strong>
      </p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'

// 氨基酸与 ΔΔG°(aa) 数据
const aminoAcids = [
  { name: 'Glu', deltaG: 2.8 },
  { name: 'Asp', deltaG: 1.9 },
  { name: 'Ala', deltaG: 1.4 },
  { name: 'Gly', deltaG: 0.7 },
  { name: 'Gln', deltaG: -1.4 },
  { name: 'Trp', deltaG: -1.1 }
]

// T-stem 三个区域的碱基对及 ΔΔG° 数据
const tStemRegions = [
  {
    region: '49-65',
    pairs: [
      { position: 'CG', deltaG: 0.0 },
      { position: 'UA', deltaG: -0.2 },
      { position: 'GC', deltaG: -0.5 },
      { position: 'AU', deltaG: -0.4 },
      { position: 'GU', deltaG: -0.9 }
    ]
  },
  {
    region: '50-64',
    pairs: [
      { position: 'GU', deltaG: 1.4 },
      { position: 'GC', deltaG: 0.4 },
      { position: 'UA', deltaG: 0.0 },
      { position: 'AU', deltaG: -0.2 }
    ]
  },
  {
    region: '51-63',
    pairs: [
      { position: 'AU', deltaG: 0.1 },
      { position: 'UG', deltaG: 0.2 },
      { position: 'GC', deltaG: -0.5 },
      { position: 'AC', deltaG: -0.8 },
      { position: 'GC', deltaG: -1.0 }
    ]
  }
]

// 响应式数据
const selectedAminoAcid = ref<number | null>(null)
const selectedPairs = ref<Record<string, number>>({
  '49-65': 0.0,
  '50-64': 0.0,
  '51-63': 0.0
})

// 计算 ΔG°(total)
const calculateTotalDeltaG = () => {
  const aminoAcidG = selectedAminoAcid.value
  const tStemG = Object.values(selectedPairs.value).reduce((a, b) => a + b, 0)

  if (aminoAcidG !== null) {
    return (aminoAcidG + tStemG).toFixed(2)
  } else {
    return null
  }
}
</script>

<style scoped>
.free-energy-evaluation {
  padding: 20px;
  font-family: Arial, sans-serif;
}

h3 {
  text-align: center;
  color: #2c3e50;
}

label {
  font-weight: bold;
}

select {
  margin: 10px 0;
  padding: 5px;
  width: 100%;
  max-width: 300px;
  font-size: 1em;
}

.result {
  margin-top: 20px;
  font-size: 1.2em;
  text-align: center;
  color: #34495e;
}

.amino-acid-selection,
.tstem-selection {
  margin-bottom: 15px;
}
</style>
