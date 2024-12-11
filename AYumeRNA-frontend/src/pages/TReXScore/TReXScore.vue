<template>
  <div class="site--main">
    <h2 class="title">tREX Score Analysis</h2>
    <p class="description">
      This section analyzes the tREX scores for the selected sequences. The tREX
      score helps to assess the quality of the sequences based on their
      similarity to template sequences.
    </p>

    <!-- 配置展示 -->
    <div class="parameters-container">
      <h3>Generation Parameters</h3>
      <p><strong>Amino Acid:</strong> {{ aminoAcid }}</p>
      <p><strong>Domain:</strong> {{ domain }}</p>

      <!-- 用户选择反密码子 -->
      <label for="reverse-codon">Select Reverse Codon:</label>
      <select
        id="reverse-codon"
        v-model="selectedReverseCodon"
        @change="onReverseCodonChange"
        class="select-box"
      >
        <option
          v-for="codon in reverseCodonOptions"
          :key="codon"
          :value="codon"
        >
          {{ codon }}
        </option>
      </select>
    </div>

    <button
      v-if="!taskSubmitted && shouldRecalculate"
      @click="submitTask"
      class="submit-btn"
    >
      Submit Task
    </button>
    <p v-else-if="taskSubmitted" class="task-status">
      Task has been submitted. Waiting for results...
    </p>

    <!-- WebSocket 消息展示 -->
    <div v-if="wsMessages.length" class="message-container">
      <h3>Push Notification</h3>
      <p class="message-item">{{ wsMessages[0] }}</p>
    </div>

    <!-- 下载按钮 -->
    <button
      v-if="resultDownloadUrl"
      @click="downloadResult"
      class="download-btn"
    >
      Download Results
    </button>

    <!-- 使用 TableWithAction 组件 -->
    <TableWithAction
      :data-source="sequences"
      @download-selected="downloadSelectedResults"
    />
  </div>
</template>

<script setup lang="ts">
import TableWithAction from './TableWithAction'
import { ref, onMounted, computed } from 'vue'
import axios from 'axios'
import { Client } from '@stomp/stompjs'
import SockJS from 'sockjs-client'

// 定义数据结构类型
interface GenerationParameters {
  model: string
  sequenceCount: number
}

interface Sequence {
  sequence: string
  trexScore: number | null
}

const generationParameters = ref<GenerationParameters>({
  model: '',
  sequenceCount: 0,
})
const aminoAcid = ref<string>('')
const domain = ref<string>('')
const sequences = ref<Sequence[]>([])
const wsMessages = ref<string[]>([])
const taskSubmitted = ref<boolean>(false)
const resultDownloadUrl = ref<string | null>(null)

// 反密码子相关
const reverseCodonOptions = ['TAA', 'TAG', 'TGA']
const selectedReverseCodon = ref<string>(reverseCodonOptions[0]) // 默认选择第一个
const reverseCodonStorageKeyPrefix = 'sequences_' // 为每个反密码子单独存储数据

// WebSocket相关配置
const wsUrl = '/sockjs/ws'
const subscribeUrl = '/topic/progress/1'

// 判断是否需要重新计算
const shouldRecalculate = computed(() => {
  if (sequences.value.length === 0) return true
  for (const seq of sequences.value) {
    if (seq.trexScore === null || !isScoreAcceptable(seq.trexScore)) {
      return true
    }
  }
  return false
})

// eslint-disable-next-line @typescript-eslint/no-unused-vars
function isScoreAcceptable(score: number): boolean {
  // 在这里根据需要修改判断逻辑
  return true
}

// 将序列转换为FA格式文件
function convertSequencesToFA(): Blob {
  const faContent = sequences.value
    .map((seq, index) => `>seq${index + 1}\n${seq.sequence}`)
    .join('\n')
  return new Blob([faContent], { type: 'text/plain' })
}

// 根据反密码子获取本地存储key
function getCodonStorageKey(codon: string) {
  return `${reverseCodonStorageKeyPrefix}${codon}`
}

// 加载指定反密码子的本地缓存
function loadSequencesForCodon(codon: string): boolean {
  const codonKey = getCodonStorageKey(codon)
  const seqData = localStorage.getItem(codonKey)
  if (seqData) {
    try {
      const parsedSequences = JSON.parse(seqData) as Sequence[]
      sequences.value = parsedSequences.map(seq => ({
        sequence: seq.sequence,
        trexScore: seq.trexScore ?? null,
      }))
      return true
    } catch (error) {
      console.error(`Error parsing sequences for ${codon}:`, error)
    }
  }
  return false
}

// 存储指定反密码子的序列数据
function storeSequencesForCodon(codon: string, seqs: Sequence[]) {
  const codonKey = getCodonStorageKey(codon)
  localStorage.setItem(codonKey, JSON.stringify(seqs))
}

// 当用户切换反密码子时调用
function onReverseCodonChange() {
  // 优先尝试从该反密码子的缓存中加载
  if (!loadSequencesForCodon(selectedReverseCodon.value)) {
    // 如果无缓存，则尝试使用默认的 'sequences' 数据作为初始数据
    const defaultSeqData = localStorage.getItem('sequences')
    if (defaultSeqData) {
      try {
        const parsedSequences = JSON.parse(defaultSeqData) as Sequence[]
        sequences.value = parsedSequences.map(seq => ({
          sequence: seq.sequence,
          trexScore: seq.trexScore ?? null,
        }))
      } catch (error) {
        console.error('Error parsing default sequences:', error)
      }
    }

    // 此时如果依然没有数据，或者数据需要重新计算，则发起新请求
    if (shouldRecalculate.value) {
      rerunScoringForSelectedCodon()
    } else {
      console.log(
        `Loaded default sequences for ${selectedReverseCodon.value}. No need to recalculate.`,
      )
    }
  } else {
    // 本地已有该反密码子的缓存数据
    taskSubmitted.value = false
    wsMessages.value = []
    resultDownloadUrl.value = null

    if (shouldRecalculate.value) {
      rerunScoringForSelectedCodon()
    } else {
      console.log(
        `Loaded cached sequences for ${selectedReverseCodon.value}. No need to recalculate.`,
      )
    }
  }
}

// 需要针对当前反密码子重新计算时调用
function rerunScoringForSelectedCodon() {
  // 清空当前数据，准备重新请求打分
  wsMessages.value = []
  resultDownloadUrl.value = null
  taskSubmitted.value = false

  connectWebSocket()
  submitTask()
}

// 从 localStorage 加载基本参数和默认序列
function loadLocalStorageData() {
  const parameters = localStorage.getItem('generationParameters')
  if (parameters) {
    try {
      const parsedParams = JSON.parse(parameters) as {
        model: string
        sequenceCount: number
      }
      generationParameters.value.model = parsedParams.model
      generationParameters.value.sequenceCount = parsedParams.sequenceCount

      const [parsedAminoAcid, parsedDomain] = generationParameters.value.model
        .replace('.pt', '')
        .split('_')
      aminoAcid.value = parsedAminoAcid
      domain.value = parsedDomain
    } catch (error) {
      console.error('Error parsing generationParameters:', error)
    }
  }

  // 从默认的 'sequences' 中加载数据（作为初始数据源）
  const sequenceData = localStorage.getItem('sequences')
  if (sequenceData) {
    try {
      const parsedSequences = JSON.parse(sequenceData) as Sequence[]
      sequences.value = parsedSequences.map(seq => ({
        sequence: seq.sequence,
        trexScore: seq.trexScore ?? null,
      }))
    } catch (error) {
      console.error('Error parsing sequences:', error)
    }
  }
}

// 提交任务，请确保 sequences 不为空（此时 sequences 已有默认值或缓存值）
async function submitTask() {
  if (!aminoAcid.value || !domain.value || !selectedReverseCodon.value) {
    console.warn('Required parameters are missing.')
    return
  }

  // 如果没有任何序列数据，需要用户确认数据来源（原始代码中默认从 localStorage 中获取，如果还没有则需要用户生成）
  if (sequences.value.length === 0) {
    console.warn(
      'Sequences are empty. Please ensure sequences are loaded or generated first.',
    )
    return
  }

  const formData = new FormData()
  formData.append('aminoAcid', aminoAcid.value)
  formData.append('domain', domain.value)
  formData.append('reverseCodon', selectedReverseCodon.value)
  formData.append('testFile', convertSequencesToFA(), 'test_sequences.fa')

  try {
    const response = await axios.post('/sequence/process', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    })
    console.log('Response from server:', response)
    wsMessages.value = ['Task submitted successfully. Waiting for results...']
    taskSubmitted.value = true
  } catch (error) {
    console.error('Error submitting task:', error)
  }
}

// 建立 WebSocket 连接
function connectWebSocket() {
  const socket = new SockJS(wsUrl)
  const client = new Client({
    webSocketFactory: () => socket,
    reconnectDelay: 5000,
    onConnect: () => {
      client.subscribe(subscribeUrl, async message => {
        const { body } = message
        wsMessages.value = [body]

        const resultUrlMatch = body.match(
          /results uploaded:\s*(https?:\/\/\S+)/,
        )
        if (resultUrlMatch) {
          const resultUrl = resultUrlMatch[1]
          resultDownloadUrl.value = resultUrl
          await fetchAndReplaceSequences(resultUrl)
        }
      })
    },
    onStompError: error => {
      console.error('WebSocket STOMP error:', error)
    },
  })
  client.activate()
}

// 下载并替换序列数据
async function fetchAndReplaceSequences(url: string) {
  try {
    const response = await axios.get<string>(url)
    const text = response.data

    const newSequences: Sequence[] = []
    const lines = text.split('\n')
    // 假设 CSV 格式：index,score,sequence (第一行为表头)
    for (let i = 1; i < lines.length; i++) {
      const line = lines[i]
      if (line.trim() === '') continue
      // eslint-disable-next-line @typescript-eslint/no-unused-vars
      const [_indexStr, scoreStr, sequence] = line.split(',')
      const trexScore = parseFloat(scoreStr.trim())
      newSequences.push({
        sequence: sequence.trim(),
        trexScore: trexScore,
      })
    }

    sequences.value = newSequences
    // 缓存当前反密码子的结果
    storeSequencesForCodon(selectedReverseCodon.value, newSequences)
  } catch (error) {
    console.error('Error fetching or replacing sequences:', error)
  }
}

// 下载选中结果
function downloadSelectedResults(selectedRows: Sequence[]) {
  if (!selectedRows.length) {
    alert('No rows selected to download.')
    return
  }
  const content = selectedRows
    .map(
      row =>
        `Sequence: ${row.sequence}, tREX Score: ${row.trexScore ?? 'Not Calculated'}`,
    )
    .join('\n')
  const blob = new Blob([content], { type: 'text/plain' })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = 'selected_results.txt'
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  URL.revokeObjectURL(url)
}

// 下载全部结果
function downloadResult() {
  if (!resultDownloadUrl.value) return
  const link = document.createElement('a')
  link.href = resultDownloadUrl.value
  link.download = 'tREX_results.csv'
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
}

// 初始化逻辑
onMounted(() => {
  // 先加载基本参数和默认序列数据
  loadLocalStorageData()

  // 如果当前数据需要重新计算，则建立连接并提交任务
  if (shouldRecalculate.value) {
    connectWebSocket()
    if (!taskSubmitted.value) {
      submitTask()
    }
  } else {
    console.log(
      'Sequences are up-to-date with acceptable trexScores. No need to recalculate.',
    )
  }
})
</script>

<style scoped>
.site--main {
  padding: 20px;
}

.parameters-container,
.sequences-container {
  margin-bottom: 20px;
}

.parameters-container p,
.sequences-container ul {
  margin: 10px 0;
}

.submit-btn,
.download-btn {
  background-color: #007bff;
  color: white;
  border: none;
  padding: 10px 20px;
  cursor: pointer;
  border-radius: 5px;
  font-weight: bold;
  margin: 10px 0;
}

.submit-btn:hover,
.download-btn:hover {
  background-color: #0056b3;
}

.task-status {
  color: green;
  font-weight: bold;
}

.message-container {
  background-color: #f9f9f9;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  padding: 16px;
}

.message-item {
  margin: 5px 0;
}

.select-box {
  margin-top: 10px;
  padding: 5px;
}
</style>
