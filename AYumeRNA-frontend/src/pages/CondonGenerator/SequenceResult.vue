<template>
  <div class="sequence-result">
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
      Next Step: Analysis
    </button>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { Client } from '@stomp/stompjs'
import SockJS from 'sockjs-client'

// 定义 Sequence 类型
type Sequence = {
  index: number
  sequence: string
}

const statusMessage = ref<string>('') // 显示任务状态消息
const progress = ref<string>('') // 显示采样进度
const resultUrl = ref<string>('') // 显示最终结果的 URL
const sequences = ref<Sequence[]>([]) // 存储生成的序列
const router = useRouter()

// 假设你已经接收到服务器返回的 `subscribeUrl`
const subscribeUrl = '/topic/progress/1'

// 在组件加载时读取本地存储的数据
onMounted(() => {
  console.log('Loading sequences from localStorage...')
  loadSequencesFromLocalStorage()
  connectWebSocket() // 连接 WebSocket
})

// 加载本地存储数据的方法
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

// 保存所有序列到 localStorage
function saveSequencesToLocalStorage() {
  console.log('Saving all sequences to localStorage:', sequences.value)
  localStorage.setItem('sequences', JSON.stringify(sequences.value))
}

// WebSocket 连接和订阅进度
function connectWebSocket() {
  console.log('Attempting to connect to WebSocket...')
  const socket = new SockJS('/sockjs/ws') // 替换为实际的 WebSocket URL
  const stompClient = new Client({
    webSocketFactory: () => socket,
    onConnect: () => {
      console.log('WebSocket connected successfully')
      stompClient.subscribe(subscribeUrl, msg => {
        const messageBody = msg.body
        console.log('Received message:', messageBody)

        // 直接作为文本显示，不进行任何解析
        statusMessage.value = messageBody
        console.log('Status message updated:', statusMessage.value)

        // 更新进度信息
        if (messageBody.includes('Progress')) {
          progress.value = messageBody // 显示进度
          console.log('Progress updated:', progress.value) // 打印进度
        }

        // 如果任务完成，检测结果 URL 并自动下载文件
        if (messageBody.includes('Sampling task completed')) {
          // 提取结果的 URL
          const match = messageBody.match(/result uploaded: (\S+)/)
          if (match) {
            resultUrl.value = match[1] // 提取 URL
            console.log('Result URL detected:', resultUrl.value) // 打印结果链接
            // 自动下载并解析 FA 文件
            downloadAndParseFile()
          } else {
            console.error('No result URL found in message:', messageBody)
          }
        }

        // 强制视图更新，确保所有数据都被渲染
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

// 下载并解析FA文件
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

    // 解析 .fa 文件格式
    const parsedSequences = parseFastaFile(text)

    // 更新序列数据
    sequences.value = parsedSequences
    console.log('Sequences parsed and updated:', sequences.value)

    // 自动保存所有序列到 localStorage
    saveSequencesToLocalStorage()
  } catch (error) {
    console.error('Error downloading or parsing the FA file:', error)
  }
}

// 解析FASTA文件格式
function parseFastaFile(fileContent: string): Sequence[] {
  console.log('Parsing FA file content:', fileContent) // 打印文件内容
  const lines = fileContent.split('\n')
  const sequences: Sequence[] = []
  let currentSequence = ''

  lines.forEach(line => {
    if (line.startsWith('>')) {
      // 如果已有序列，保存它
      if (currentSequence) {
        sequences.push({
          index: sequences.length + 1,
          sequence: currentSequence,
        })
      }
      // 开始新序列
      currentSequence = ''
    } else {
      // 添加到当前序列
      currentSequence += line.trim()
    }
  })

  // 保存最后一个序列
  if (currentSequence) {
    sequences.push({ index: sequences.length + 1, sequence: currentSequence })
  }

  return sequences
}

// 跳转到分析页面
function goToAnalysis() {
  // 在点击分析时保存当前的序列
  console.log('Saving all sequences before navigation:', sequences.value)

  saveSequencesToLocalStorage()

  console.log('Navigating to analysis page')
  router.push({ name: 'TReXScore' })
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
