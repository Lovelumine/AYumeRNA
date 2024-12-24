<template>
  <div class="sequence-result">
    <h3>Sampling Task Status</h3>
    <p v-if="statusMessage">{{ statusMessage }}</p>

    <!-- 这里也有进度条，但可以留空或只做调试用途 -->
    <el-progress
      v-if="progressValue !== null && !resultUrl"
      :text-inside="true"
      :stroke-width="20"
      :percentage="progressValue"
      status="active"
      color="#4caf50"
      style="margin: 1em auto; width: 50%;"
    />

    <p v-if="resultUrl">
      Sampling task completed!
      <a :href="resultUrl" target="_blank" @click="downloadAndParseFile">
        Click to download the result
      </a>
    </p>

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

/**
 * 定义要 emit 的事件
 * "progress-updated" => 父组件更新进度
 * "task-completed"   => 父组件进度直接到 100
 */
const emits = defineEmits<{
  (e: 'progress-updated', value: number): void
  (e: 'task-completed'): void
}>()

type Sequence = {
  index: number
  sequence: string
}

const statusMessage = ref('')
const progress = ref('')
const progressValue = ref<number|null>(null)
const resultUrl = ref<string>('')
const sequences = ref<Sequence[]>([])
const router = useRouter()

const subscribeUrl = '/topic/progress/1'

onMounted(() => {
  loadSequencesFromLocalStorage()
  connectWebSocket()
})

function loadSequencesFromLocalStorage() {
  const storedSequences = localStorage.getItem('sequences')
  if (storedSequences) {
    try {
      sequences.value = JSON.parse(storedSequences)
    } catch (error) {
      console.error('Failed to parse sequences from localStorage:', error)
    }
  }
}

function saveSequencesToLocalStorage() {
  localStorage.setItem('sequences', JSON.stringify(sequences.value))
}

/**
 * 连接 WebSocket 并监听进度消息
 */
function connectWebSocket() {
  console.log('SequenceResult: connecting WebSocket...')
  const socket = new SockJS('/sockjs/ws')
  const stompClient = new Client({
    webSocketFactory: () => socket,
    onConnect: () => {
      stompClient.subscribe(subscribeUrl, msg => {
        const messageBody = msg.body
        console.log('SequenceResult received message:', messageBody)
        statusMessage.value = messageBody

        // 若包含 "Progress XX%"
        if (messageBody.includes('Progress')) {
          progress.value = messageBody
          const match = messageBody.match(/Progress\s+(\d+)%/)
          if (match) {
            // 拿到服务器进度
            const newProg = parseInt(match[1], 10)
            progressValue.value = newProg

            // emit 给父组件 => 父组件更新进度
            emits('progress-updated', newProg)
          }
        }

        // 若包含 "Sampling task completed"
        if (messageBody.includes('Sampling task completed')) {
          const match = messageBody.match(/result uploaded: (\S+)/)
          if (match) {
            resultUrl.value = match[1]
            downloadAndParseFile()
          }
          // 告诉父组件 => 任务完成
          emits('task-completed')
        }

        nextTick(() => {
          console.log('SequenceResult: nextTick -> data updated.')
        })
      })
    },
  })

  stompClient.activate()
}

/**
 * 下载并解析结果文件
 */
async function downloadAndParseFile() {
  if (!resultUrl.value) return
  try {
    const resp = await fetch(resultUrl.value)
    const text = await resp.text()
    console.log('File text:', text)

    const parsed = parseFastaFile(text)
    sequences.value = parsed
    saveSequencesToLocalStorage()
  } catch (error) {
    console.error('Error parsing file:', error)
  }
}

/**
 * 解析FASTA文本
 */
function parseFastaFile(fileContent: string): Sequence[] {
  const lines = fileContent.split('\n')
  const result: Sequence[] = []
  let currentSeq = ''

  for (const line of lines) {
    if (line.startsWith('>')) {
      if (currentSeq) {
        result.push({ index: result.length + 1, sequence: currentSeq })
      }
      currentSeq = ''
    } else {
      currentSeq += line.trim()
    }
  }

  if (currentSeq) {
    result.push({ index: result.length + 1, sequence: currentSeq })
  }
  return result
}

/**
 * 跳转到下个页面
 */
function goToAnalysis() {
  saveSequencesToLocalStorage()
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
  margin-top: 1em;
  transition: background-color 0.3s ease;
}

.analysis-btn:hover {
  background-color: #45a049;
}
</style>
