<template>
  <div class="sequence-result">
    <h3>Sampling Task Status</h3>
    <p v-if="statusMessage">{{ statusMessage }}</p>

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

    <button v-if="sequences.length" class="analysis-btn" @click="goToAnalysis">
      Next Step: Evaluator
    </button>

    <!-- Using @shene/table component with locale -->
    <STableProvider :locale="en">
      <s-table
        v-if="sequences.length"
        :columns="columns"
        :data-source="sequences"
        :pagination="pagination"
        style="width: 100%"
        rowKey="sequence"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'sequence'">
            <span>{{ record.sequence }}</span>
          </template>
        </template>
      </s-table>
    </STableProvider>

    <!-- Countdown block -->
    <div v-if="countdown > 0" class="countdown">
      Next step will start in {{ countdown }} seconds.
      <button class="cancel-btn" @click="cancelCountdown">Cancel Countdown</button>
    </div>

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
import en from '@shene/table/dist/locale/en'  // Correctly import the 'en' locale
import type { STableColumnsType, STablePaginationConfig } from '@shene/table'

/**
 * Define events to emit
 * "progress-updated" => Updates the progress in the parent component
 * "task-completed"   => Sets progress to 100% in the parent component
 */
const emits = defineEmits<{
  (e: 'progress-updated', value: number): void
  (e: 'task-completed'): void
}>()

type Sequence = {
  key: string // Use unique key field
  index: number
  sequence: string
}

const statusMessage = ref('')
const progressValue = ref<number|null>(null)
const resultUrl = ref<string>('')
const sequences = ref<Sequence[]>([])
const router = useRouter()

// Table column configuration
const columns: STableColumnsType<Sequence> = [
  { title: 'Index', dataIndex: 'index', key: 'index', width: 10 },
  { title: 'Sequence', dataIndex: 'sequence', key: 'sequence', width: 800 },
]

// Pagination configuration
const pagination: STablePaginationConfig = {
  defaultCurrent: 1,
  defaultPageSize: 5,
  showQuickJumper: true,
  showSizeChanger: true,
  showTotal: total => `Total ${total} items`,
  pageSizeOptions: ['5', '10', '20', '50']
}

const subscribeUrl = '/topic/progress/1'

onMounted(() => {
  loadSequencesFromLocalStorage()
  connectWebSocket()
})

// Countdown reactive state and timer
const countdown = ref(0)
let countdownTimer: number | null = null

function startCountdown(seconds: number = 10) {
  if (countdownTimer !== null) return // Already counting down
  countdown.value = seconds
  countdownTimer = window.setInterval(() => {
    countdown.value--
    if (countdown.value <= 0) {
      clearInterval(countdownTimer!)
      countdownTimer = null
      goToAnalysis()  // Automatically navigate to next step when countdown ends
    }
  }, 1000)
}

function cancelCountdown() {
  if (countdownTimer !== null) {
    clearInterval(countdownTimer)
    countdownTimer = null
  }
  countdown.value = 0
  console.log("Countdown cancelled")
}

// Load sequences from local storage
function loadSequencesFromLocalStorage() {
  const storedSequences = localStorage.getItem('sequences')
  if (storedSequences) {
    try {
      sequences.value = JSON.parse(storedSequences)
      console.log("Loaded sequences from localStorage:", sequences.value)
    } catch (error) {
      console.error('Failed to parse sequences from localStorage:', error)
    }
  }
}

// Save sequences to local storage
function saveSequencesToLocalStorage() {
  console.log('Saving sequences to localStorage:', sequences.value)
  localStorage.setItem('sequences', JSON.stringify(sequences.value))
}

/**
 * Connect WebSocket and listen to progress messages
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

        // If "Progress XX%" is present
        if (messageBody.includes('Progress')) {
          const match = messageBody.match(/Progress\s+(\d+)%/)
          if (match) {
            const newProg = parseInt(match[1], 10)
            progressValue.value = newProg
            console.log('Progress updated:', newProg)
          }
        }

        // If "Sampling task completed" is present
        if (messageBody.includes('Sampling task completed')) {
          const match = messageBody.match(/result uploaded: (\S+)/)
          if (match) {
            resultUrl.value = match[1]
            downloadAndParseFile()
          }
          // Notify parent component => task completed
          emits('task-completed')
          // Start the countdown after task is completed
          startCountdown(10)
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
 * Download and parse the result file
 */
async function downloadAndParseFile() {
  if (!resultUrl.value) return
  try {
    const resp = await fetch(resultUrl.value)
    const text = await resp.text()
    console.log('File text:', text)

    const parsed = parseFastaFile(text)
    sequences.value = parsed
    console.log('Parsed sequences:', sequences.value)
    saveSequencesToLocalStorage()
  } catch (error) {
    console.error('Error parsing file:', error)
  }
}

/**
 * Parse the FASTA file
 */
function parseFastaFile(fileContent: string): Sequence[] {
  const lines = fileContent.split('\n')
  const result: Sequence[] = []
  let currentSeq = ''

  for (const line of lines) {
    if (line.startsWith('>')) {
      if (currentSeq) {
        result.push({ key: `seq-${result.length + 1}`, index: result.length + 1, sequence: currentSeq })
      }
      currentSeq = ''
    } else {
      currentSeq += line.trim()
    }
  }

  if (currentSeq) {
    result.push({ key: `seq-${result.length + 1}`, index: result.length + 1, sequence: currentSeq })
  }

  console.log('Parsed sequence data:', result)
  return result
}

/**
 * Go to the next page (Evaluator)
 */
function goToAnalysis() {
  saveSequencesToLocalStorage()
  router.push({ name: 'Structure Folding Evaluation' })
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

.countdown {
  margin-top: 1em;
  font-size: 1.2em;
  color: #333;
}

.cancel-btn {
  margin-left: 1em;
  padding: 0.4em 0.8em;
  background-color: #f44336;
  color: white;
  border: none;
  border-radius: 3px;
  cursor: pointer;
  font-size: 0.9em;
}

.cancel-btn:hover {
  background-color: #d32f2f;
}
</style>
