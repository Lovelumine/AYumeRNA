<template>
  <div class="site--main">
    <h2 class="title">tREX Score Analysis</h2>
    <p class="description">
      This section analyzes the tREX scores for the selected sequences.
    </p>

    <div class="parameters-container">
      <h3>Generation Parameters</h3>
      <p><strong>Amino Acid:</strong> {{ aminoAcid }}</p>
      <p><strong>Domain:</strong> {{ domain }}</p>

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

    <button v-if="!taskSubmitted" @click="submitTask" class="submit-btn">
      Submit Task
    </button>
    <p v-else-if="taskSubmitted" class="task-status">
      Task has been submitted. Waiting for results...
    </p>

    <div v-if="wsMessages.length" class="message-container">
      <h3>Push Notification</h3>
      <p class="message-item">{{ wsMessages[0] }}</p>
    </div>

    <button
      v-if="resultDownloadUrl"
      @click="downloadResult"
      class="download-btn"
    >
      Download Results
    </button>

    <TableWithAction
      :data-source="sequences"
      @download-selected="downloadSelectedResults"
    />
  </div>
</template>

<script setup lang="ts">
import TableWithAction from './TableWithAction'
import { ref, onMounted } from 'vue'
import axios from 'axios'
import { Client } from '@stomp/stompjs'
import SockJS from 'sockjs-client'

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

const reverseCodonOptions = ['CUA', 'UUA']
const selectedReverseCodon = ref<string>(reverseCodonOptions[0])

// 用于记录本次提交是针对哪个codon的
let lastSubmittedCodon: string | null = null

const wsUrl = '/sockjs/ws'
const subscribeUrl = '/topic/progress/1'

// eslint-disable-next-line @typescript-eslint/no-unused-vars
function isScoreAcceptable(score: number): boolean {
  return true
}

function getCodonStorageKey(codon: string) {
  return `sequences_${codon}`
}

function getCodonTimestampKey(codon: string) {
  return `timestamp_${codon}`
}

function sequencesToFA(seqArr: Sequence[]): string {
  return seqArr
    .map((seq, index) => `>seq${index + 1}\n${seq.sequence}`)
    .join('\n')
}

// 尝试加载默认 sequences（未打分）
function loadDefaultSequences(): boolean {
  const defaultSeqDataStr = localStorage.getItem('sequences')
  if (!defaultSeqDataStr) return false
  try {
    const parsed = JSON.parse(defaultSeqDataStr) as Sequence[]
    sequences.value = parsed.map(s => ({
      sequence: s.sequence,
      trexScore: s.trexScore ?? null,
    }))
    console.log(`Loaded default sequences, length=${parsed.length}`)
    return true
  } catch (error) {
    console.error('Error parsing default sequences:', error)
    return false
  }
}

// 提交任务使用当前 sequences
function submitTask() {
  console.log('submitTask called.')
  if (sequences.value.length === 0) {
    console.log('No sequences available, no submit.')
    return
  }

  // 记录本次提交的codon
  lastSubmittedCodon = selectedReverseCodon.value

  const formData = new FormData()
  formData.append('aminoAcid', aminoAcid.value)
  formData.append('domain', domain.value)
  formData.append('reverseCodon', selectedReverseCodon.value)
  formData.append(
    'testFile',
    new Blob([sequencesToFA(sequences.value)], { type: 'text/plain' }),
    'test_sequences.fa',
  )

  axios
    .post('/sequence/process', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    })
    .then(response => {
      console.log('Response from server:', response)
      wsMessages.value = ['Task submitted successfully. Waiting for results...']
      taskSubmitted.value = true
    })
    .catch(error => {
      console.error('Error submitting task:', error)
    })
}

// 连接 WebSocket
function connectWebSocket() {
  console.log('Connecting WebSocket...')
  const socket = new SockJS(wsUrl)
  const client = new Client({
    webSocketFactory: () => socket,
    reconnectDelay: 5000,
    onConnect: () => {
      console.log('WebSocket connected.')
      client.subscribe(subscribeUrl, async message => {
        const { body } = message
        console.log('Received WS message:', body)
        wsMessages.value = [body]

        const resultUrlMatch = body.match(
          /results uploaded:\s*(https?:\/\/\S+)/,
        )
        if (resultUrlMatch) {
          const resultUrl = resultUrlMatch[1]
          const proxiedUrl = resultUrl.replace(
            'https://minio.lumoxuan.cn/ayumerna',
            '/ayumerna',
          )
          console.log('Fetching new sequences from:', proxiedUrl)
          await fetchAndReplaceSequences(proxiedUrl)
          taskSubmitted.value = false
        }
      })
    },
    onStompError: error => {
      console.error('WebSocket STOMP error:', error)
    },
  })
  client.activate()
}

// fetchAndReplaceSequences完成后仅更新lastSubmittedCodon对应的数据
async function fetchAndReplaceSequences(url: string) {
  console.log('fetchAndReplaceSequences from:', url)
  try {
    const response = await axios.get<string>(url)
    const text = response.data

    const newSequences: Sequence[] = []
    const lines = text.split('\n')
    for (let i = 1; i < lines.length; i++) {
      const line = lines[i].trim()
      if (line === '') continue
      // eslint-disable-next-line @typescript-eslint/no-unused-vars
      const [testSeq, scoreStr, sequence] = line.split(',')
      const trexScore = parseFloat(scoreStr.trim())
      newSequences.push({ sequence: sequence.trim(), trexScore: trexScore })
    }

    console.log('New scored sequences fetched:', JSON.stringify(newSequences))
    sequences.value = newSequences

    const currentTimestampSequences = localStorage.getItem(
      'timestamp_sequences',
    )
    if (currentTimestampSequences && lastSubmittedCodon) {
      // 只更新本次提交对应的codon数据，不影响其他codon
      localStorage.setItem(
        getCodonStorageKey(lastSubmittedCodon),
        JSON.stringify(newSequences),
      )
      localStorage.setItem(
        getCodonTimestampKey(lastSubmittedCodon),
        currentTimestampSequences,
      )
      console.log(
        'Updated codon scored sequences and timestamp for',
        lastSubmittedCodon,
      )

      // 重置lastSubmittedCodon，避免下次不匹配
      lastSubmittedCodon = null
    }
  } catch (error) {
    console.error('Error fetching or replacing sequences:', error)
  }
}

function downloadSelectedResults(selectedRows: Sequence[]) {
  if (!selectedRows.length) {
    alert('No rows selected to download.')
    return
  }
  console.log('Downloading selected rows:', selectedRows)
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

function downloadResult() {
  if (!resultDownloadUrl.value) return
  console.log('Downloading result from:', resultDownloadUrl.value)
  const link = document.createElement('a')
  link.href = resultDownloadUrl.value
  link.download = 'tREX_results.csv'
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
}

// 切换codon时逻辑
function onReverseCodonChange() {
  console.log(`Changing reverse codon to ${selectedReverseCodon.value}`)

  const currentTimestampSequences = localStorage.getItem('timestamp_sequences')
  const codonTimestamp = localStorage.getItem(
    getCodonTimestampKey(selectedReverseCodon.value),
  )
  const codonSeqData = localStorage.getItem(
    getCodonStorageKey(selectedReverseCodon.value),
  )

  if (codonSeqData) {
    // 有codon打分序列
    try {
      const parsed = JSON.parse(codonSeqData) as Sequence[]
      sequences.value = parsed.map(s => ({
        sequence: s.sequence,
        trexScore: s.trexScore ?? null,
      }))
      console.log(
        `Loaded scored codon sequences for ${selectedReverseCodon.value}, length=${parsed.length}`,
      )

      if (!codonTimestamp) {
        // 没有codon timestamp，表示需要更新数据，用默认 sequences
        console.log('No codon timestamp, submitTask using default sequences.')
        if (loadDefaultSequences()) {
          connectWebSocket()
          submitTask()
        } else {
          console.log('No default sequences, do nothing.')
        }
      } else if (
        currentTimestampSequences &&
        currentTimestampSequences !== codonTimestamp
      ) {
        // 时间戳不一致，用默认 sequences 提交
        console.log(
          'timestamp_sequences differs from codon timestamp, submitTask using default sequences.',
        )
        if (loadDefaultSequences()) {
          connectWebSocket()
          submitTask()
        } else {
          console.log('No default sequences, do nothing.')
        }
      } else {
        // 时间戳一致，无需提交
        console.log('Codon sequences and timestamp match, no submit.')
      }
    } catch (error) {
      console.error(
        `Error parsing codon ${selectedReverseCodon.value} sequences:`,
        error,
      )
    }
  } else {
    // 无codon打分序列，看是否有默认序列
    const hasDefault = loadDefaultSequences()
    if (hasDefault) {
      console.log(
        'No codon scored sequences, but have default sequences, submitTask.',
      )
      connectWebSocket()
      submitTask()
    } else {
      console.log('No sequences at all, do nothing.')
    }
  }

  wsMessages.value = []
  localStorage.setItem('selectedReverseCodon', selectedReverseCodon.value)
}

onMounted(() => {
  console.log('onMounted: Loading local storage data...')

  const parametersStr = localStorage.getItem('generationParameters')
  if (parametersStr) {
    try {
      const parsedParams = JSON.parse(parametersStr) as {
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

  const savedCodon = localStorage.getItem('selectedReverseCodon')
  if (savedCodon && reverseCodonOptions.includes(savedCodon)) {
    selectedReverseCodon.value = savedCodon
  } else {
    selectedReverseCodon.value = reverseCodonOptions[0]
  }

  const hasDefault = loadDefaultSequences()
  if (!hasDefault) {
    // 没有 sequences，不提交也不操作
    console.log('No sequences found, do nothing.')
    return
  }

  const currentTimestampSequences = localStorage.getItem('timestamp_sequences')
  const codonTimestamp = localStorage.getItem(
    getCodonTimestampKey(selectedReverseCodon.value),
  )

  // 初次进入页面对比timestamp
  if (!codonTimestamp) {
    console.log('No codon timestamp, submitTask using default sequences.')
    connectWebSocket()
    submitTask()
  } else if (
    currentTimestampSequences &&
    currentTimestampSequences !== codonTimestamp
  ) {
    console.log(
      'timestamp_sequences differs from codon timestamp, submitTask using default sequences.',
    )
    connectWebSocket()
    submitTask()
  } else {
    console.log('Everything matches, try loading codon scored sequences.')
    const codonSeqData = localStorage.getItem(
      getCodonStorageKey(selectedReverseCodon.value),
    )
    if (codonSeqData) {
      try {
        const parsed = JSON.parse(codonSeqData) as Sequence[]
        sequences.value = parsed.map(s => ({
          sequence: s.sequence,
          trexScore: s.trexScore ?? null,
        }))
        console.log(
          `Loaded scored codon sequences for ${selectedReverseCodon.value}, length=${parsed.length}`,
        )
      } catch (error) {
        console.error(
          `Error parsing codon ${selectedReverseCodon.value} sequences:`,
          error,
        )
      }
    } else {
      console.log('No codon scored sequences found, using default as is.')
    }
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
