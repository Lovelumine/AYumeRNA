<!-- src/pages/TReXScore/TRNACompatibilityEvaluator.vue -->
<template>
  <div class="site--main">
    <h2 class="title">tRNACompatibility Evaluator</h2>
    <p class="description">
      Welcome to the <strong>tRNACompatibility Evaluator</strong>. In the first step, we used AI to generate novel sup-tRNA sequences. Now, we will evaluate these sequences to determine whether they can carry specific amino acids and read through stop codons.
    </p>

    <div class="info-box">
  <h3>Overview</h3>
  <p>
    In the first step, sup-tRNA sequences with special suppressor capabilities were generated using AI. Now, in this second phase, we apply the tREX Score algorithm to evaluate these tRNAs and determine if they can carry specific amino acids and read through stop codons.
  </p>

  <details class="details-box">
    <summary class="details-summary">
      Show More About the First Step
    </summary>
    <div class="details-content">
      <h4>Step 1: Generating sup-tRNA Sequences</h4>
      <p>
        In the first step, computational models and reference datasets were used to generate new sup-tRNA sequences with potential stop codon suppression capabilities. At this stage, the specific amino acids they carry were not yet determined. These sequences serve as candidates for further evaluation in this phase.
      </p>
    </div>
  </details>
</div>

<div class="info-box">
  <h3>Step 2: Evaluating tRNAs with tREX Score</h3>
  <p>
    To determine whether the generated sup-tRNA sequences can carry specific amino acids and suppress stop codons, we use the tREX Score algorithm. This process aligns candidate tRNAs against consensus templates derived from reference tRNA datasets.
  </p>
  <details class="details-box">
    <summary class="details-summary">
      Show More About the Second Step
    </summary>
    <p>The reference datasets for scoring are:</p>
    <ul>
      <li>
        Ala (Alanine):
        <a
          href="https://minio.lumoxuan.cn/ayumerna/model/Ala.csv"
          target="_blank"
          >Ala.csv</a
        >
      </li>
      <li>
        Arg (Arginine):
        <a
          href="https://minio.lumoxuan.cn/ayumerna/model/Arg.csv"
          target="_blank"
          >Arg.csv</a
        >
      </li>
      <li>
        Asn (Asparagine):
        <a
          href="https://minio.lumoxuan.cn/ayumerna/model/Asn.csv"
          target="_blank"
          >Asn.csv</a
        >
      </li>
      <li>
        Asp (Aspartic Acid):
        <a
          href="https://minio.lumoxuan.cn/ayumerna/model/Asp.csv"
          target="_blank"
          >Asp.csv</a
        >
      </li>
      <li>
        Cys (Cysteine):
        <a
          href="https://minio.lumoxuan.cn/ayumerna/model/Cys.csv"
          target="_blank"
          >Cys.csv</a
        >
      </li>
      <li>
        Gly (Glycine):
        <a
          href="https://minio.lumoxuan.cn/ayumerna/model/Gly.csv"
          target="_blank"
          >Gly.csv</a
        >
      </li>
      <li>
        His (Histidine):
        <a
          href="https://minio.lumoxuan.cn/ayumerna/model/His.csv"
          target="_blank"
          >His.csv</a
        >
      </li>
      <li>
        Trp (Tryptophan):
        <a
          href="https://minio.lumoxuan.cn/ayumerna/model/Trp.csv"
          target="_blank"
          >Trp.csv</a
        >
      </li>
      <li>
        Val (Valine):
        <a
          href="https://minio.lumoxuan.cn/ayumerna/model/Val.csv"
          target="_blank"
          >Val.csv</a
        >
      </li>
    </ul>
    <p>
      These files contain template tRNA sequences used to generate a consensus template and identify conserved positions.
    </p>

    <p>
      After performing a multiple sequence alignment (MSA) on the templates, we define:
    </p>
    <img
      src="https://minio.lumoxuan.cn/ayumerna/picture/formula_1.png"
      alt="Formula 1"
      class="formula-image-1"
    />

    <p>
      For each test sequence, after aligning it to the consensus sequence, each conserved position <math>i ∈ C</math> is scored as follows:
    </p>
    <img
      src="https://minio.lumoxuan.cn/ayumerna/picture/formula_2.png"
      alt="Formula 2"
      class="formula-image-2"
    />

    <p>Finally, the tREX Score is computed as:</p>
    <img
      src="https://minio.lumoxuan.cn/ayumerna/picture/formula_3.png"
      alt="Formula 3"
      class="formula-image-3"
    />
  </details>
  <p>
    A positive tREX Score indicates that the tRNA may carry a specific amino acid and possess suppressor properties, making it a true sup-tRNA capable of decoding stop codons.
  </p>
</div>

<div class="parameters-container">
      <h3>Generation Parameters</h3>
      <p><strong>Anticodon:</strong> {{ aminoAcid }}</p>
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

    <p v-if="taskSubmitted" class="task-status">
      Task has been submitted. Waiting for results...
    </p>

    <!-- <div v-if="wsMessages.length" class="message-container">
      <h3>Push Notification</h3>
      <p class="message-item">{{ wsMessages[0] }}</p>
    </div> -->

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
import TableWithAction from './TableWithAction' // 根据实际路径调整
import { ref, onMounted } from 'vue'
import { Client } from '@stomp/stompjs'
import SockJS from 'sockjs-client'
import type { Sequence, GenerationParameters } from './logic'
import {
  getCodonStorageKey,
  getCodonTimestampKey,
  loadDefaultSequences,
  fetchAndReplaceSequences,
  submitTask,
  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  lastSubmittedCodon,
} from './logic'

// 定义响应式变量
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

const reverseCodonOptions = ['Ala','Arg','Asn','Asp','Cys','Gly','Trp','His','Val']
const selectedReverseCodon = ref<string>(reverseCodonOptions[0])

const wsUrl = '/sockjs/ws'
const subscribeUrl = '/topic/progress/1'

// 下载选中的结果
function downloadSelectedResults(selectedRows: Sequence[]) {
  if (!selectedRows.length) {
    alert('No rows selected to download.')
    return
  }
  console.log('Downloading selected rows:', selectedRows)
  const content = selectedRows
    .map(
      row =>
        `Sequence: ${row.sequence}, tREX Score: ${row.trexScore ?? 'Waiting'}`,
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
  console.log('Downloading result from:', resultDownloadUrl.value)
  const link = document.createElement('a')
  link.href = resultDownloadUrl.value
  link.download = 'tREX_results.csv'
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
}

let clientInstance: Client | null = null

// 连接 WebSocket
function connectWebSocket() {
  console.log('Connecting WebSocket...')
  const socket = new SockJS(wsUrl)
  clientInstance = new Client({
    webSocketFactory: () => socket,
    reconnectDelay: 5000,
    onConnect: () => {
      console.log('WebSocket connected.')
      if (clientInstance) {
        clientInstance.subscribe(subscribeUrl, async message => {
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
            await fetchAndReplaceSequences(proxiedUrl, sequences)
            taskSubmitted.value = false
          }
        })
      }
    },
    onStompError: error => {
      console.error('WebSocket STOMP error:', error)
    },
  })
  clientInstance.activate()
}

// 反向密码子切换时的逻辑
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
        console.log('No codon timestamp, submitTask using default sequences.')
        if (loadDefaultSequences(sequences)) {
          submitTask(
            aminoAcid.value,
            domain.value,
            selectedReverseCodon.value,
            sequences,
            wsMessages,
            taskSubmitted,
            connectWebSocket,
          )
        } else {
          console.log('No default sequences, do nothing.')
        }
      } else if (
        currentTimestampSequences &&
        currentTimestampSequences !== codonTimestamp
      ) {
        console.log(
          'timestamp_sequences differs from codon timestamp, submitTask using default sequences.',
        )
        if (loadDefaultSequences(sequences)) {
          submitTask(
            aminoAcid.value,
            domain.value,
            selectedReverseCodon.value,
            sequences,
            wsMessages,
            taskSubmitted,
            connectWebSocket,
          )
        } else {
          console.log('No default sequences, do nothing.')
        }
      } else {
        console.log('Codon sequences and timestamp match, no submit.')
      }
    } catch (error) {
      console.error(
        `Error parsing codon ${selectedReverseCodon.value} sequences:`,
        error,
      )
    }
  } else {
    // 无codon打分序列
    const hasDefault = loadDefaultSequences(sequences)
    if (hasDefault) {
      console.log(
        'No codon scored sequences, but have default sequences, submitTask.',
      )
      submitTask(
        aminoAcid.value,
        domain.value,
        selectedReverseCodon.value,
        sequences,
        wsMessages,
        taskSubmitted,
        connectWebSocket,
      )
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

  const hasDefault = loadDefaultSequences(sequences)
  if (!hasDefault) {
    console.log('No sequences found, do nothing.')
    return
  }

  const currentTimestampSequences = localStorage.getItem('timestamp_sequences')
  const codonTimestamp = localStorage.getItem(
    getCodonTimestampKey(selectedReverseCodon.value),
  )

  if (!codonTimestamp) {
    console.log('No codon timestamp, submitTask using default sequences.')
    submitTask(
      aminoAcid.value,
      domain.value,
      selectedReverseCodon.value,
      sequences,
      wsMessages,
      taskSubmitted,
      connectWebSocket,
    )
  } else if (
    currentTimestampSequences &&
    currentTimestampSequences !== codonTimestamp
  ) {
    console.log(
      'timestamp_sequences differs from codon timestamp, submitTask using default sequences.',
    )
    submitTask(
      aminoAcid.value,
      domain.value,
      selectedReverseCodon.value,
      sequences,
      wsMessages,
      taskSubmitted,
      connectWebSocket,
    )
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
  font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
  line-height: 1.6;

  margin: 0 auto;
}

.title {
  font-size: 2.5em;
  margin-bottom: 10px;
  text-align: center;
  color: #333;
}

.description {
  font-size: 1.2em;
  text-align: center;
  margin-bottom: 30px;
  color: #555;
}

.info-box {
  background: #f7f7f7;
  border: 1px solid #ddd;
  border-radius: 8px;
  padding: 20px;
  margin-bottom: 20px;
  box-shadow: 0 2px 5px rgba(0, 0, 0, 0.1);
}

.parameters-container {
  background: #f7f7f7;
  border: 1px solid #ddd;
  border-radius: 8px;
  padding: 20px;
  margin-bottom: 20px;
  box-shadow: 0 2px 5px rgba(0, 0, 0, 0.1);
}

.parameters-container h3 {
  margin-bottom: 10px;
  color: #333;
}

.parameters-container p {
  margin: 8px 0;
  color: #555;
}

.select-box {
  padding: 8px;
  margin-top: 10px;
  border-radius: 4px;
  border: 1px solid #ccc;
  width: 100%;
  max-width: 200px;
}

.submit-btn,
.download-btn {
  background-color: #007bff;
  color: white;
  border: none;
  padding: 12px 24px;
  cursor: pointer;
  border-radius: 5px;
  font-weight: bold;
  transition: background-color 0.3s ease;
  margin: 10px 0;
  display: block;
  width: fit-content;
}

.submit-btn:hover,
.download-btn:hover {
  background-color: #0056b3;
}

.task-status {
  color: green;
  font-weight: bold;
  margin-top: 10px;
}

.message-container {
  background-color: #f9f9f9;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  padding: 16px;
  margin-top: 20px;
  box-shadow: 0 2px 5px rgba(0, 0, 0, 0.1);
}

.message-container h3 {
  margin-top: 0;
  color: #333;
}

.message-item {
  margin: 5px 0;
  color: #555;
}

.details-box {
  margin-top: 10px;
  cursor: pointer;
}

.details-summary {
  font-weight: bold;
  color: #007bff;
  outline: none;
  font-size: 1em;
}

.details-summary:hover {
  text-decoration: underline;
}

.details-content {
  margin-top: 10px;
  color: #555;
}

.formula-image-1 {
  display: block;
  margin: 10px auto;
  height: auto;
  max-width: 70%; /* 根据图片原比例缩放 */
  width: auto;
}

.formula-image-2 {
  display: block;
  margin: 10px auto;
  height: auto;
  max-width: 25%; /* 根据图片原比例缩放 */
  width: auto;
}

.formula-image-3 {
  display: block;
  margin: 10px auto;
  height: auto;
  max-width: 30%; /* 根据图片原比例缩放 */
  width: auto;
}
</style>
