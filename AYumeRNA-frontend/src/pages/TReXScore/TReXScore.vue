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
      <p>
        <strong>Reverse Codon:</strong> {{ generationParameters.reverseCodon }}
      </p>
      <p>
        <strong>Sequence Count:</strong>
        {{ generationParameters.sequenceCount }}
      </p>
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
    <TableWithAction :data-source="sequences" />
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
  reverseCodon: string
  sequenceCount: number
}

interface Sequence {
  sequence: string
  trexScore: number | null
}

// 初始化变量
const generationParameters = ref<GenerationParameters>({
  model: '',
  reverseCodon: '',
  sequenceCount: 0,
})
const aminoAcid = ref<string>('') // 从 model 中解析出的 aminoAcid
const domain = ref<string>('') // 从 model 中解析出的 domain
const sequences = ref<Sequence[]>([])
const wsMessages = ref<string[]>([]) // WebSocket 消息
const taskSubmitted = ref<boolean>(false) // 任务是否已提交
const resultDownloadUrl = ref<string | null>(null) // 下载结果的链接

const wsUrl = '/sockjs/ws' // 替换为实际 WebSocket 地址
const subscribeUrl = '/topic/progress/1' // 替换为实际订阅路径

// 计算属性，判断是否需要重新计算
const shouldRecalculate = computed(() => {
  if (sequences.value.length === 0) {
    return true
  }
  for (const seq of sequences.value) {
    if (seq.trexScore === null || !isScoreAcceptable(seq.trexScore)) {
      return true
    }
  }
  return false
})

// 判断 trexScore 是否符合条件的函数（请根据您的标准修改）
// eslint-disable-next-line @typescript-eslint/no-unused-vars
function isScoreAcceptable(score: number): boolean {
  // 在这里实现您的判断逻辑
  // 例如：return score >= 0.8;
  return true // 示例，默认返回 true
}

// 将序列转换为 FA 格式
const convertSequencesToFA = (): Blob => {
  const faContent = sequences.value
    .map((seq, index) => `>seq${index + 1}\n${seq.sequence}`)
    .join('\n')
  return new Blob([faContent], { type: 'text/plain' })
}

// 从 localStorage 加载数据
const loadLocalStorageData = () => {
  const parameters = localStorage.getItem('generationParameters')
  const sequenceData = localStorage.getItem('sequences')

  if (parameters) {
    try {
      generationParameters.value = JSON.parse(
        parameters,
      ) as GenerationParameters

      const [parsedAminoAcid, parsedDomain] = generationParameters.value.model
        .replace('.pt', '')
        .split('_')
      aminoAcid.value = parsedAminoAcid
      domain.value = parsedDomain
    } catch (error) {
      console.error('Error parsing generationParameters:', error)
    }
  }

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

// 提交任务
const submitTask = async () => {
  if (
    !aminoAcid.value ||
    !domain.value ||
    !generationParameters.value.reverseCodon
  ) {
    console.warn('Required parameters are missing.')
    return
  }

  const formData = new FormData()
  formData.append('aminoAcid', aminoAcid.value)
  formData.append('domain', domain.value)
  formData.append('reverseCodon', generationParameters.value.reverseCodon)
  formData.append('testFile', convertSequencesToFA(), 'test_sequences.fa')

  try {
    const response = await axios.post('/sequence/process', formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    })
    console.log('Response from server:', response)
    wsMessages.value = ['Task submitted successfully. Waiting for results...']
    taskSubmitted.value = true // 标记任务已提交
  } catch (error) {
    console.error('Error submitting task:', error)
  }
}

// WebSocket 连接
const connectWebSocket = () => {
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
          resultDownloadUrl.value = resultUrl // 设置下载链接
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
const fetchAndReplaceSequences = async (url: string) => {
  try {
    const response = await axios.get<string>(url)
    const text = response.data

    const newSequences: Sequence[] = []

    const lines = text.split('\n')
    // 假设 CSV 格式：index,score,sequence
    for (let i = 1; i < lines.length; i++) {
      // 跳过表头
      const line = lines[i]
      if (line.trim() === '') continue
      // eslint-disable-next-line @typescript-eslint/no-unused-vars
      const [indexStr, scoreStr, sequence] = line.split(',')
      const trexScore = parseFloat(scoreStr.trim())
      newSequences.push({
        sequence: sequence.trim(),
        trexScore: trexScore,
      })
    }

    sequences.value = newSequences
    localStorage.setItem('sequences', JSON.stringify(newSequences))
  } catch (error) {
    console.error('Error fetching or replacing sequences:', error)
  }
}

// 提供下载功能
const downloadResult = () => {
  if (!resultDownloadUrl.value) return
  const link = document.createElement('a')
  link.href = resultDownloadUrl.value
  link.download = 'tREX_results.csv'
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
}

// 初始化
onMounted(() => {
  loadLocalStorageData()
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
</style>
