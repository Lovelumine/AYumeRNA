<template>
  <div class="structure-folding-evaluation">
    <h3 class="title">Structure Folding Evaluation</h3>
    <p class="description">
      The table below displays the structure folding parameters of each sequence, including anticodon, infernal score, and tRNA positions.
      These parameters are key to understanding the structural features of the tRNA.
    </p>

    <!-- 使用 shane 表格展示 -->
    <s-table-provider :locale="en">
      <s-table
        :columns="columns"
        :data-source="dataSource"
        :max-height="300"
        row-key="key"
      >
        <template #bodyCell="{ text, column, record }">
          <template v-if="column.key === 'sequence'">
            <a>{{ text }}</a>
          </template>
          <template v-else-if="column.key === 'anticodon'">
            {{ record.anticodon }}
          </template>
          <template v-else-if="column.key === 'infernalScore'">
            {{ record.infernalScore }}
          </template>
          <template v-else-if="column.key === 'tRNAStart'">
            {{ record.tRNAStart }}
          </template>
          <template v-else-if="column.key === 'tRNAEnd'">
            {{ record.tRNAEnd }}
          </template>
          <template v-else-if="column.key === 'tRNAType'">
            {{ record.tRNAType }}
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
import axios from 'axios'
import { STableColumnsType } from '@shene/table'
import en from '@shene/table/dist/locale/en'

// 类型定义
interface SequenceInfo {
  key: string
  sequence: string
  anticodon: string | null
  infernalScore: string | null
  tRNAStart: string | null
  tRNAEnd: string | null
  tRNAType: string | null
}

interface Sequence {
  key: string
  sequence: string
}

const columns: STableColumnsType<SequenceInfo> = [
  {
    title: 'Sequence',
    dataIndex: 'sequence',
    key: 'sequence',
    width: 200,
  },
  {
    title: 'Anticodon',
    dataIndex: 'anticodon',
    key: 'anticodon',
    width: 180,
  },
  {
    title: 'Infernal Score',
    dataIndex: 'infernalScore',
    key: 'infernalScore',
    width: 180,
  },
  {
    title: 'tRNA Begin',
    dataIndex: 'tRNAStart',
    key: 'tRNAStart',
    width: 180,
  },
  {
    title: 'tRNA End',
    dataIndex: 'tRNAEnd',
    key: 'tRNAEnd',
    width: 180,
  },
  {
    title: 'tRNA Type',
    dataIndex: 'tRNAType',
    key: 'tRNAType',
    width: 180,
  },
]

const sequences = ref<Sequence[]>([]) // 存储序列数据
const dataSource = ref<SequenceInfo[]>([]) // 表格的数据源
const loading = ref<boolean>(false) // 控制加载状态
const error = ref<string | null>(null) // 错误提示

// 加载存储的序列
const loadSequences = () => {
  const storedData = localStorage.getItem('sequences')
  if (!storedData) {
    error.value = 'No sequences found in local storage.'
    return
  }
  sequences.value = JSON.parse(storedData) as Sequence[]
  // 初始化 dataSource 数组为 'Loading...'
  dataSource.value = sequences.value.map((seq, index) => ({
    key: seq.key || index.toString(), // 确保 key 被正确设置
    sequence: seq.sequence,
    anticodon: 'Loading...',
    infernalScore: 'Loading...',
    tRNAStart: 'Loading...',
    tRNAEnd: 'Loading...',
    tRNAType: 'Loading...',
  }))
}

// 发送请求并解析返回结果
const fetchSequenceInfo = async (index: number) => {
  try {
    const seq = sequences.value[index]
    const response = await axios.post('/run', { sequence: seq.sequence })

    // 解析结果并更新
    const data = response.data[0] || {}
    dataSource.value[index].anticodon = data.Anticodon || 'N/A'
    dataSource.value[index].infernalScore = data['Infernal Score'] || 'N/A'
    dataSource.value[index].tRNAStart = data['tRNA Begin'] || 'N/A'
    dataSource.value[index].tRNAEnd = data['tRNA End'] || 'N/A'
    dataSource.value[index].tRNAType = data['tRNA Type'] || 'N/A'
  } catch (err) {
    console.error('Error fetching sequence info for index ' + index, err)
    dataSource.value[index].anticodon = 'Error'
    dataSource.value[index].infernalScore = 'Error'
    dataSource.value[index].tRNAStart = 'Error'
    dataSource.value[index].tRNAEnd = 'Error'
    dataSource.value[index].tRNAType = 'Error'
  }
}

// 延时函数
const delay = (ms: number) => new Promise(resolve => setTimeout(resolve, ms))


// 每次请求间隔 0.5 秒
const fetchAllSequenceInfo = async () => {
  loading.value = true
  for (let index = 0; index < sequences.value.length; index++) {
    await fetchSequenceInfo(index)  // 请求每个序列的结构折叠参数
    await delay(500)  // 每次请求后等待 0.5 秒
  }
  loading.value = false
}

onMounted(() => {
  loadSequences()
  if (sequences.value.length > 0) {
    fetchAllSequenceInfo() // 开始请求
  }
})
</script>

<style scoped>
.title, .description {
  text-align: center;
}

.loading, .error {
  text-align: center;
  margin-top: 20px;
}

.error {
  color: red;
}

.loading {
  color: #409eff;
}
</style>
