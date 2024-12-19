<!-- src/pages/TRNAEvaluator/AminoacylationEvaluation/AminoacylationEvaluation.vue -->
<template>
  <div class="free-energy-evaluation">
    <h3 class="title">The affinity between aa-tRNAs and EF-Tu</h3>
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
        @pagination-change="onPaginationChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'basePairs'">
            <span>{{ (record.basePairs || []).slice(0, 3).join(', ') || 'Loading...' }}</span>
          </template>
          <template v-else-if="column.key === 'freeEnergy'">
            <span>{{ record.freeEnergy ?? 'Loading...' }}</span>
          </template>
          <!-- 移除直接渲染 totalFreeEnergy 的部分 -->
          <template v-else-if="column.key === 'actions'">
            <button class="action-btn" @click="handleAnalyzeSequence(record)">
              Visualization
            </button>
          </template>
        </template>
      </s-table>
    </s-table-provider>

    <!-- 加载和错误提示 -->
    <div v-if="error" class="error">{{ error }}</div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import en from '@shene/table/dist/locale/en'
import { columns, pagination, onPaginationChange, type SequenceData } from './tableConfig'
import axios from 'axios'
import { aminoAcids, calculateFreeEnergy, handleAnalyzeSequence } from './computedata'

// 响应式变量
const dataSource = ref<SequenceData[]>([])
const loading = ref(false)
const error = ref<string | null>(null)
const selectedAminoAcid = ref<number>(aminoAcids[0].deltaG) // 默认选中第一个

// 加载序列数据并缓存结果
async function loadSequences() {
  const defaultSeqDataStr = localStorage.getItem('sequences')
  const timestamp = localStorage.getItem('timestamp_sequences')
  const cachedDataStr = localStorage.getItem('cached_sequences')
  const cachedTimestamp = localStorage.getItem('cached_timestamp_sequences')

  if (!defaultSeqDataStr) {
    error.value = 'No sequences found in local storage.'
    return
  }

  const parsedSequences = JSON.parse(defaultSeqDataStr) as { sequence: string }[]

  // 如果缓存有效，加载缓存数据
  if (cachedDataStr && timestamp === cachedTimestamp) {
    console.log('[INFO] Using cached sequence data.')
    dataSource.value = JSON.parse(cachedDataStr)
    return
  }

  console.log('[INFO] Cache invalid, recalculating sequence data.')

  // 初始化数据
  dataSource.value = parsedSequences.map((seq, index) => ({
    key: index.toString(),
    sequence: seq.sequence,
    tstemSequence: '',
    tstemPosition: '',
    basePairs: [],
    freeEnergy: '',
    totalFreeEnergy: 0, // 初始化为0
  }))

  loading.value = true
  try {
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
        // 计算并赋值 totalFreeEnergy
        const parsedFreeEnergy = parseFloat(seq.freeEnergy)
        seq.totalFreeEnergy = isNaN(parsedFreeEnergy) ? 0 : calculateTotalEnergy(parsedFreeEnergy)
      } catch (e) {
        console.error(`[ERROR] Failed to process sequence: ${seq.sequence}`, e)
        seq.tstemSequence = 'Error'
        seq.tstemPosition = 'Error'
        seq.basePairs = []
        seq.freeEnergy = 'Error'
        seq.totalFreeEnergy = 0 // 出错时设置为0或其他默认值
      }
    }

    // 缓存计算结果和时间戳
    localStorage.setItem('cached_sequences', JSON.stringify(dataSource.value))
    localStorage.setItem('cached_timestamp_sequences', timestamp || '')
    console.log('[INFO] Sequence data cached successfully.')
  } catch (e) {
    console.error('[ERROR] Failed to calculate sequence data:', e)
    error.value = 'Failed to calculate sequence data.'
  } finally {
    loading.value = false
  }
}

// 计算 Total Free Energy
function calculateTotalEnergy(freeEnergy: number): number {
  return freeEnergy + selectedAminoAcid.value
}

// 监听 selectedAminoAcid 的变化，重新计算 totalFreeEnergy
// eslint-disable-next-line @typescript-eslint/no-unused-vars
watch(selectedAminoAcid, (newVal) => {
  dataSource.value.forEach(record => {
    const freeEnergy = parseFloat(record.freeEnergy)
    record.totalFreeEnergy = isNaN(freeEnergy) ? 0 : calculateTotalEnergy(freeEnergy)
  })
})

// 页面加载时执行
onMounted(() => {
  loadSequences()
})

// 处理分析序列的函数（保持不变）
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

.action-btn {
  padding: 4px 8px;
  color: #fff;
  background-color: #409eff;
  border: none;
  border-radius: 4px;
  cursor: pointer;
}

.action-btn:hover {
  background-color: #66b1ff;
}
</style>
