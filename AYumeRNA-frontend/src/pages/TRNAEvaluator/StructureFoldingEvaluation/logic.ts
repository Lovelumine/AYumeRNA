import { ref } from 'vue'
import axios from 'axios'
import type { SequenceInfo } from './tableConfig.ts'
import router from '@/router/index.js'

// 类型定义
interface Sequence {
  key: string
  sequence: string
}

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

// 存储序列并跳转到 /visualization
export function handleAnalyzeSequence(record: SequenceInfo) {
  localStorage.setItem('analyzedSequence', JSON.stringify(record))
  router.push('/visualization').then(() => {
    console.log('Navigated to /visualization')
  })
}

export {
  sequences,
  dataSource,
  loading,
  error,
  loadSequences,
  fetchAllSequenceInfo
}
