<template>
  <div class="free-energy-evaluation">
    <h3 class="title">Aminoacylation Evaluation</h3>
    <p class="description">
      Select an amino acid to dynamically update the free energy contributions for each sequence.
      The total free energy includes the amino acid ΔΔG° and contributions from the tRNA T-stem base pairs.
      A total free energy close to 0 indicates an optimal balance.
    </p>

    <!-- 氨基酸选择框 -->
    <div class="amino-acid-selection">
      <label for="amino-acid" class="label">Select Amino Acid:</label>
      <div class="custom-select">
        <select v-model="selectedAminoAcid">
          <option v-for="aa in aminoAcids" :key="aa.name" :value="aa.deltaG">
            {{ aa.name }} (ΔΔG°: {{ aa.deltaG }})
          </option>
        </select>
      </div>
    </div>

    <!-- 结果表格 -->
    <s-table-provider :locale="en">
    <s-table
    :columns="columns"
    :data-source="dataSource"
    :max-height="500"
    :pagination="pagination"
    @pagination-change="onPaginationChange">
      <template #bodyCell="{ column, record }">
        <!-- 自定义列显示 -->
        <template v-if="column.key === 'basePairs'">
          <span>{{ (record.basePairs || []).slice(0, 3).join(', ') || 'Loading...' }}</span>
        </template>
        <template v-else-if="column.key === 'freeEnergy'">
          <span>{{ record.freeEnergy ?? 'Loading...' }}</span>
        </template>
        <template v-else-if="column.key === 'totalFreeEnergy'">
          <span class="total-energy">{{ calculateTotalEnergy(record.freeEnergy) }}</span>
        </template>
      </template>
    </s-table>
  </s-table-provider>
    <!-- 加载和错误提示 -->
    <div v-if="error" class="error">{{ error }}</div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import en from '@shene/table/dist/locale/en'
import { columns, pagination, onPaginationChange, type SequenceData } from './tableConfig'
import axios from 'axios'
import { aminoAcids, calculateFreeEnergy} from './computedata';



// 响应式变量
const dataSource = ref<SequenceData[]>([])
const loading = ref(false)
const error = ref<string | null>(null)
const selectedAminoAcid = ref<number>(aminoAcids[0].deltaG) // 默认选中第一个



// 加载序列数据
async function loadSequences() {
  const defaultSeqDataStr = localStorage.getItem('sequences')
  if (!defaultSeqDataStr) {
    error.value = 'No sequences found in local storage.'
    return
  }

  const parsedSequences = JSON.parse(defaultSeqDataStr) as { sequence: string }[]
  dataSource.value = parsedSequences.map((seq, index) => ({
    key: index.toString(),
    sequence: seq.sequence,
    tstemSequence: '',
    tstemPosition: '',
    basePairs: [],
    freeEnergy: ''
  }))

  loading.value = true
  for (const seq of dataSource.value) {
    try {
      const response = await axios.post('/charge', { sequence: seq.sequence })
      const data = response.data
      const tstemSequence = data['T-stem Sequence'] || ''
      const { basePairs, energy } = calculateFreeEnergy(tstemSequence)

      seq.tstemSequence = tstemSequence
      seq.tstemPosition = data['T-stem Position'] || ''
      seq.basePairs = basePairs
      seq.freeEnergy = energy.toFixed(2)
    // eslint-disable-next-line @typescript-eslint/no-unused-vars
    } catch (e) {
      seq.tstemSequence = 'Error'
    }
  }
  loading.value = false
}

// 计算 Total Free Energy
function calculateTotalEnergy(freeEnergy?: string): string {
  if (!freeEnergy) return 'Loading...'
  return (parseFloat(freeEnergy) + selectedAminoAcid.value).toFixed(2)
}

// 页面加载时执行
onMounted(() => {
  loadSequences()
})
</script>

<style scoped>
.title, .description {
  text-align: center;
}

.amino-acid-selection {
  display: flex;
  justify-content: center;
  align-items: center;
  margin: 20px 0;
}

.custom-select select {
  padding: 8px 12px;
  font-size: 16px;
  border: 1px solid #ccc;
  border-radius: 4px;
  outline: none;
  transition: border-color 0.3s ease;
  width: 300px;
}

.custom-select select:focus {
  border-color: #409eff;
}

.label {
  margin-right: 10px;
  font-weight: bold;
  color: #333;
}

.total-energy {
  font-weight: bold;
  color: #2c3e50;
}

.loading, .error {
  text-align: center;
  margin-top: 20px;
}
</style>
