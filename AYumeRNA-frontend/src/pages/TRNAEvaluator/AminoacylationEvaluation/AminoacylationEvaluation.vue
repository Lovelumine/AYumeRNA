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
import type { STableColumnsType, STablePaginationConfig, STableProps } from '@shene/table'
import axios from 'axios'

// 氨基酸与 ΔΔG° 数据
const aminoAcids = [
  { name: 'Glu', deltaG: 2.8 },
  { name: 'Asp', deltaG: 1.9 },
  { name: 'Ala', deltaG: 1.4 },
  { name: 'Gly', deltaG: 0.7 },
  { name: 'Gln', deltaG: -1.4 },
  { name: 'Trp', deltaG: -1.1 }
]

// T-stem 碱基对 ΔΔG° 数据表
const positionBasedDeltaG: Record<string, Record<string, number>> = {
  "49-65": { CG: 0.0, UA: -0.2, GC: -0.4, AU: -0.5, GU: -0.9 },
  "50-64": { GU: 1.4, UG: 0.4, GC: 0.0, AU: 0.0, UA: 0.0, CG: -0.2 },
  "51-63": { AU: 0.1, UA: 0.0, UG: 0.0, GU: -0.2, CG: -0.5, AC: -0.8, GC: -1.0 }
}

// 类型定义
interface SequenceData {
  key: string
  sequence: string
  tstemSequence: string
  tstemPosition: string
  basePairs: string[]
  freeEnergy: string
}

// 表格列定义
const columns: STableColumnsType<SequenceData> = [
  { title: 'Sequence', dataIndex: 'sequence', key: 'sequence', width: 200, ellipsis: true },
  { title: 'T-stem Sequence', dataIndex: 'tstemSequence', key: 'tstemSequence', width: 80, ellipsis: true },
  { title: 'T-stem Position', dataIndex: 'tstemPosition', key: 'tstemPosition', width: 80, ellipsis: true },
  { title: 'Base Pairs', key: 'basePairs', width: 80, ellipsis: true },
  { title: 'Free Energy (kcal/mol)', key: 'freeEnergy', width: 150, ellipsis: true },
  { title: 'Total Free Energy (kcal/mol)', key: 'totalFreeEnergy', width: 150, ellipsis: true }
]

// 响应式变量
const dataSource = ref<SequenceData[]>([])
const loading = ref(false)
const error = ref<string | null>(null)
const selectedAminoAcid = ref<number>(aminoAcids[0].deltaG) // 默认选中第一个

// 计算自由能
function calculateFreeEnergy(tstemSequence: string): { basePairs: string[]; energy: number } {
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

// 分页配置
const pagination = ref<STablePaginationConfig>({
	defaultCurrent: 1, // 默认当前页
	defaultPageSize: 5, // 默认每页显示5条
	showQuickJumper: true, // 显示快速跳转输入框
	showSizeChanger: true, // 允许改变每页条数
	showTotal: total => `Total ${total} items`, // 显示总条数信息
	pageSizeOptions: ['5', '10', '20', '50'] // 每页可选条数
})

// 分页事件
const onPaginationChange: STableProps['onPaginationChange'] = params => {
	console.log('分页参数:', params)
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
