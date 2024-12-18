// src/pages/TRNAEvaluator/StructureFoldingEvaluation/logic.ts

import { ref } from 'vue'
import axios from 'axios'
import type { SequenceInfo } from './tableConfig.ts'
import router from '@/router/index.js'

// 类型定义
interface Sequence {
  key: string
  sequence: string
}

// 缓存数据的接口
interface CachedSequenceInfo {
  anticodon: string | null
  infernalScore: string | null
  tRNAStart: string | null
  tRNAEnd: string | null
  tRNAType: string | null
}

// 常量定义
const SEQUENCES_KEY = 'sequences'
const TIMESTAMP_KEY = 'timestamp_sequences'

// 状态变量
const sequences = ref<Sequence[]>([]) // 存储序列数据
const dataSource = ref<SequenceInfo[]>([]) // 表格的数据源
const loading = ref<boolean>(false) // 控制加载状态
const error = ref<string | null>(null) // 错误提示

// 获取当前时间戳
const getCurrentTimestamp = () => new Date().toISOString()

// 生成缓存键
const getCacheKey = (timestamp: string, key: string) => `sequence_info_${timestamp}_${key}`

// 加载存储的序列并处理缓存
const loadSequences = () => {
  const storedSequences = localStorage.getItem(SEQUENCES_KEY)
  const storedTimestamp = localStorage.getItem(TIMESTAMP_KEY)

  if (!storedSequences || !storedTimestamp) {
    error.value = '本地存储中未找到序列或时间戳。'
    return
  }

  try {
    sequences.value = JSON.parse(storedSequences) as Sequence[]
  } catch (parseError) {
    console.error('解析本地存储中的序列时出错', parseError)
    error.value = '无法解析本地存储中的序列。'
    return
  }

  // 初始化 dataSource
  dataSource.value = sequences.value.map((seq, index) => ({
    key: seq.key || index.toString(),
    sequence: seq.sequence,
    anticodon: 'Loading...',
    infernalScore: 'Loading...',
    tRNAStart: 'Loading...',
    tRNAEnd: 'Loading...',
    tRNAType: 'Loading...',
  }))

  // 加载已缓存的序列信息
  sequences.value.forEach((seq, index) => {
    const cacheKey = getCacheKey(storedTimestamp, seq.key || index.toString())
    const cachedInfo = localStorage.getItem(cacheKey)
    if (cachedInfo) {
      try {
        const parsedInfo: CachedSequenceInfo = JSON.parse(cachedInfo)
        dataSource.value[index].anticodon = parsedInfo.anticodon || 'N/A'
        dataSource.value[index].infernalScore = parsedInfo.infernalScore || 'N/A'
        dataSource.value[index].tRNAStart = parsedInfo.tRNAStart || 'N/A'
        dataSource.value[index].tRNAEnd = parsedInfo.tRNAEnd || 'N/A'
        dataSource.value[index].tRNAType = parsedInfo.tRNAType || 'N/A'
      } catch (parseError) {
        console.error(`解析缓存的序列信息（键：${cacheKey}）时出错`, parseError)
      }
    }
  })

  // 检查是否需要重新获取未缓存的序列信息
  const needsFetch = dataSource.value.some(info => info.anticodon === 'Loading...')
  if (needsFetch) {
    fetchAllSequenceInfo(storedTimestamp)
  }
}

// 发送请求并解析返回结果
const fetchSequenceInfo = async (index: number, timestamp: string) => {
  try {
    const seq = sequences.value[index]
    const response = await axios.post('/run', { sequence: seq.sequence })

    // 解析结果并更新
    const data = response.data[0] || {}
    const info: CachedSequenceInfo = {
      anticodon: data.Anticodon || 'N/A',
      infernalScore: data['Infernal Score'] || 'N/A',
      tRNAStart: data['tRNA Begin'] || 'N/A',
      tRNAEnd: data['tRNA End'] || 'N/A',
      tRNAType: data['tRNA Type'] || 'N/A',
    }

    dataSource.value[index].anticodon = info.anticodon
    dataSource.value[index].infernalScore = info.infernalScore
    dataSource.value[index].tRNAStart = info.tRNAStart
    dataSource.value[index].tRNAEnd = info.tRNAEnd
    dataSource.value[index].tRNAType = info.tRNAType

    // 缓存单个序列的信息
    const cacheKey = getCacheKey(timestamp, seq.key || index.toString())
    localStorage.setItem(cacheKey, JSON.stringify(info))
  } catch (err) {
    console.error(`获取序列信息（索引：${index}）时出错`, err)
    dataSource.value[index].anticodon = 'Error'
    dataSource.value[index].infernalScore = 'Error'
    dataSource.value[index].tRNAStart = 'Error'
    dataSource.value[index].tRNAEnd = 'Error'
    dataSource.value[index].tRNAType = 'Error'
  }
}

// 延时函数
const delay = (ms: number) => new Promise(resolve => setTimeout(resolve, ms))

// 每次请求间隔 0.5 秒，并在完成后缓存数据
const fetchAllSequenceInfo = async (currentTimestamp: string) => {
  loading.value = true
  for (let index = 0; index < sequences.value.length; index++) {
    const info = dataSource.value[index]
    if (info.anticodon === 'Loading...') {
      await fetchSequenceInfo(index, currentTimestamp)  // 请求每个序列的结构折叠参数
      await delay(500)  // 每次请求后等待 0.5 秒
    }
  }
  loading.value = false
}

// 存储序列并跳转到 /visualization
export function handleAnalyzeSequence(record: SequenceInfo) {
  localStorage.setItem('analyzedSequence', JSON.stringify(record))
  router.push('/visualization').then(() => {
    console.log('导航到 /visualization')
  })
}

// 更新序列和时间戳
export function updateSequences(newSequences: Sequence[]) {
  const timestamp = getCurrentTimestamp()
  localStorage.setItem(SEQUENCES_KEY, JSON.stringify(newSequences))
  localStorage.setItem(TIMESTAMP_KEY, timestamp)
  // 清除之前的缓存
  newSequences.forEach((seq, index) => {
    const cacheKey = getCacheKey(timestamp, seq.key || index.toString())
    localStorage.removeItem(cacheKey)
  })
  // 更新状态
  sequences.value = newSequences
  dataSource.value = newSequences.map((seq, index) => ({
    key: seq.key || index.toString(),
    sequence: seq.sequence,
    anticodon: 'Loading...',
    infernalScore: 'Loading...',
    tRNAStart: 'Loading...',
    tRNAEnd: 'Loading...',
    tRNAType: 'Loading...',
  }))
  fetchAllSequenceInfo(timestamp)
}

export {
  sequences,
  dataSource,
  loading,
  error,
  loadSequences,
  fetchAllSequenceInfo,
}
