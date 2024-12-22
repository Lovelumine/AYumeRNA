<template>
  <div class="sequence-result" >
    <h3>Sampling Task Status</h3>

    <!-- 显示任务状态消息 -->
    <p v-if="statusMessage">{{ statusMessage }}</p>

    <!-- 最终结果展示 -->
    <p v-if="resultUrl">
      Sampling task completed! The result is available.
      <a :href="resultUrl" target="_blank" @click="downloadAndParseFile">
        Click to download the result
      </a>
    </p>

    <!-- 显示本地存储的序列数据 -->
    <el-table v-if="sequences.length" :data="sequences" style="width: 100%">
      <el-table-column prop="index" label="Index" width="80">
        <template #default="scope">
          {{ scope.$index + 1 }}
        </template>
      </el-table-column>
      <el-table-column prop="sequence" label="Sequence">
        <template #default="scope">
          {{ scope.row.sequence }}
        </template>
      </el-table-column>
    </el-table>

    <p class="select-note" v-if="sequences.length">
      The sequences are ready for analysis.
    </p>
    <button v-if="sequences.length" class="analysis-btn" @click="goToAnalysis">
      Next Step: Evaluator
    </button>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { Client } from '@stomp/stompjs'
import SockJS from 'sockjs-client'

type Sequence = {
  index: number
  sequence: string
}

const statusMessage = ref<string>('')
const progress = ref<string>('')
const resultUrl = ref<string>('')
const sequences = ref<Sequence[]>([])
const router = useRouter()

const subscribeUrl = '/topic/progress/1'

onMounted(() => {
  console.log('Loading sequences from localStorage...')
  loadSequencesFromLocalStorage()
  connectWebSocket()
})

function loadSequencesFromLocalStorage() {
  const storedSequences = localStorage.getItem('sequences')
  if (storedSequences) {
    try {
      sequences.value = JSON.parse(storedSequences)
      console.log('Loaded sequences from localStorage:', sequences.value)
    } catch (error) {
      console.error('Failed to parse sequences from localStorage:', error)
    }
  } else {
    console.log('No sequences found in localStorage.')
  }
}

function saveSequencesToLocalStorage() {
  console.log('Saving all sequences to localStorage:', sequences.value)
  localStorage.setItem('sequences', JSON.stringify(sequences.value))
}

function connectWebSocket() {
  console.log('Attempting to connect to WebSocket...')
  const socket = new SockJS('/sockjs/ws')
  const stompClient = new Client({
    webSocketFactory: () => socket,
    onConnect: () => {
      console.log('WebSocket connected successfully')
      stompClient.subscribe(subscribeUrl, msg => {
        const messageBody = msg.body
        console.log('Received message:', messageBody)
        statusMessage.value = messageBody
        console.log('Status message updated:', statusMessage.value)

        if (messageBody.includes('Progress')) {
          progress.value = messageBody
          console.log('Progress updated:', progress.value)
        }

        if (messageBody.includes('Sampling task completed')) {
          const match = messageBody.match(/result uploaded: (\S+)/)
          if (match) {
            resultUrl.value = match[1]
            console.log('Result URL detected:', resultUrl.value)

            // 替换URL中 https://minio.lumoxuan.cn/ayumerna/ 为 /ayumerna/
            if (
              resultUrl.value.includes('https://minio.lumoxuan.cn/ayumerna/')
            ) {
              resultUrl.value = resultUrl.value.replace(
                'https://minio.lumoxuan.cn/ayumerna/',
                '/ayumerna/',
              )
              console.log('Transformed result URL to:', resultUrl.value)
            }

            downloadAndParseFile()
          } else {
            console.error('No result URL found in message:', messageBody)
          }
        }

        nextTick(() => {
          console.log('Next tick completed, data should be updated.')
        })
      })
    },
    onStompError: error => {
      console.error('STOMP Error:', error)
    },
    onWebSocketClose: () => {
      console.log('WebSocket connection closed')
    },
    onWebSocketError: error => {
      console.error('WebSocket Error:', error)
    },
  })

  stompClient.activate()
}

async function downloadAndParseFile() {
  if (!resultUrl.value) {
    console.log('No result URL found, skipping file download.')
    return
  }

  console.log('Attempting to download file from URL:', resultUrl.value)
  try {
    const response = await fetch(resultUrl.value)
    const text = await response.text()
    console.log('File downloaded successfully, parsing content.')

    const parsedSequences = parseFastaFile(text)
    sequences.value = parsedSequences
    console.log('Sequences parsed and updated:', sequences.value)

    saveSequencesToLocalStorage()
  } catch (error) {
    console.error('Error downloading or parsing the FA file:', error)
  }
}

function parseFastaFile(fileContent: string): Sequence[] {
  console.log('Parsing FA file content:', fileContent)
  const lines = fileContent.split('\n')
  const sequences: Sequence[] = []
  let currentSequence = ''

  lines.forEach(line => {
    if (line.startsWith('>')) {
      if (currentSequence) {
        sequences.push({
          index: sequences.length + 1,
          sequence: currentSequence,
        })
      }
      currentSequence = ''
    } else {
      currentSequence += line.trim()
    }
  })

  if (currentSequence) {
    sequences.push({ index: sequences.length + 1, sequence: currentSequence })
  }

  return sequences
}

function goToAnalysis() {
  console.log('Saving all sequences before navigation:', sequences.value)
  saveSequencesToLocalStorage()
  console.log('Navigating to analysis page')
  router.push({ name: 'tRNA Evaluator' })
}
</script>

<style scoped>
.sequence-result {
  text-align: center;
  background-color: #ffffff;
  padding: 1.5em;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  max-width: 100%;
  margin-top: 1.5em;
  width: 100%;
}

h3 {
  color: #2c3e50;
  font-size: 1.5em;
  margin-bottom: 0.5em;
}

.progress-info {
  font-size: 1.1em;
  color: #4caf50;
  margin-top: 1em;
}

.select-note {
  font-size: 1.1em;
  color: #555;
  margin-top: 1em;
  text-align: center;
}

.analysis-btn {
  background-color: #4caf50;
  color: white;
  padding: 0.8em 1.5em;
  border: none;
  border-radius: 5px;
  font-size: 1em;
  font-weight: bold;
  cursor: pointer;
  transition: background-color 0.3s ease;
  margin-top: 1em;
}

.analysis-btn:hover {
  background-color: #45a049;
}
</style>
