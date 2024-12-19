<!-- src/pages/TRNAEvaluator/IdentityElementsEvaluation/IdentityElementsEvaluation.vue -->

<template>
  <div class="site--main">
    <div class="info-box">
      <h3>Evaluating tRNA Identity Elements with tREX Score</h3>

      <p>
        To assess the identity elements of generated sup-tRNA sequences, we use the <strong>tREX Score</strong> algorithm. This scoring method compares candidate tRNAs against consensus templates derived from validated reference tRNA datasets to identify conserved positions critical for amino acid binding and identity recognition.
      </p>

      <p>
        A <strong>positive tREX Score</strong> indicates that the generated tRNA likely possesses the correct identity elements to carry a specific amino acid and function as a suppressor tRNA (sup-tRNA). These tRNAs are capable of decoding stop codons and participating in protein synthesis with the desired amino acid specificity.
      </p>

      <details class="details-box">
        <summary class="details-summary">
          Show More About the Evaluation Process
        </summary>
        <p>
          Identity elements are critical sequence and structural features in tRNAs that determine their recognition by aminoacyl-tRNA synthetases (aaRSs) and their ability to carry specific amino acids. Proper identification of these elements is essential to evaluate whether a generated sup-tRNA sequence can function correctly in translation, including the suppression of stop codons.
        </p>

        <p>The reference datasets for scoring identity elements are as follows:</p>
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
          These files contain consensus tRNA sequences used to align and identify conserved nucleotide positions contributing to tRNA identity.
        </p>

        <p>
          After performing a multiple sequence alignment (MSA) on the templates, the following scoring formulas are applied:
        </p>

        <img
          src="https://minio.lumoxuan.cn/ayumerna/picture/formula_1.png"
          alt="Formula 1"
          class="formula-image-1"
        />

        <p>
          For each test tRNA sequence, conserved positions <math>i ∈ C</math> are scored as follows:
        </p>
        <img
          src="https://minio.lumoxuan.cn/ayumerna/picture/formula_2.png"
          alt="Formula 2"
          class="formula-image-2"
        />

        <p>
          Finally, the cumulative tREX Score is computed as:
        </p>
        <img
          src="https://minio.lumoxuan.cn/ayumerna/picture/formula_3.png"
          alt="Formula 3"
          class="formula-image-3"
        />
      </details>
    </div>

    <div class="parameters-container">
      <h3>Generation Parameters</h3>
      <p><strong>Codon:</strong> {{ aminoAcid }}</p>
      <p><strong>Domain:</strong> {{ domain }}</p>

      <label for="reverse-codon">Select Amino Acid:</label>
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
      <!-- Task has been submitted. Waiting for results... -->
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

// 移除 downloadSelectedResults 函数，因为下载在子组件中处理
// 如果该函数在其他地方使用，请保留

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

<style>
/* 你的样式内容 */
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

.action-btn {
  padding: 4px 8px;
  color: #fff;
  background-color: #409eff;
  border: none;
  border-radius: 4px;
  cursor: pointer;
}

.action-btn:hover {
  background-color: #66b1ff;
}
</style>
