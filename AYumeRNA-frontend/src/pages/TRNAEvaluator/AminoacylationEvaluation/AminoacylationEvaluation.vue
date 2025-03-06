<template>
  <div class="free-energy-evaluation">
    <h3 class="title">The affinity between aa-tRNAs and release factor</h3>
    <p class="description">
      Select an amino acid to dynamically update the free energy contributions for each sequence.
      The total free energy includes the amino acid ΔΔG° and contributions from the tRNA T-stem base pairs.
      A total free energy close to 0 indicates an optimal balance.
    </p>



    <!-- Amino Acid Selection -->
    <div class="amino-acid-selection">
      <label for="amino-acid" class="label">Select Amino Acid:</label>
      <div class="custom-select">
        <select v-model="selectedAminoAcid">
          <option v-for="aa in aminoAcids" :key="aa.name" :value="aa.deltaG">
            {{ aa.name }} (ΔΔG°: {{ aa.deltaG }})
          </option>
        </select>
      </div>
    </div>

    <!-- Action Buttons -->
    <div class="action-buttons">
      <button class="action-btn" @click="handleSelectAll">Select All</button>
      <button class="action-btn" @click="handleClearSelection">Clear Selection</button>
      <button class="action-btn red-button" @click="handleSelectAbsoluteLessThanOne">
        Select Absolute Value &lt; 1
      </button>
      <button
        class="action-btn"
        @click="handleDownloadSelected"
        :disabled="selectedRows.length === 0"
        :style="{
          backgroundColor: selectedRows.length > 0 ? 'green' : '#c0c4cc',
          borderColor: selectedRows.length > 0 ? 'green' : '#c0c4cc'
        }"
      >
        Download Selected Sequences
      </button>
    </div>

    <!-- Result Table -->
    <s-table-provider :locale="en">
      <s-table
        :columns="columns"
        :data-source="dataSource"
        :max-height="500"
        :pagination="pagination"
        row-key="key"
        :row-selection="rowSelection"
        @pagination-change="onPaginationChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'basePairs'">
            <span>{{ (record.basePairs || []).slice(0, 3).join(', ') || 'Loading...' }}</span>
          </template>
          <template v-else-if="column.key === 'freeEnergy'">
            <span>{{ record.freeEnergy ?? 'Loading...' }}</span>
          </template>
          <template v-else-if="column.key === 'actions'">
            <button class="action-btn" @click="handleAnalyzeSequence(record)">
              Visualization
            </button>
          </template>
        </template>
      </s-table>
    </s-table-provider>

    <!-- Loading and Error Messages -->
    <div v-if="error" class="error">{{ error }}</div>

    <!-- Custom Modal -->
    <div v-if="showCustomModal" class="modal-overlay" @click.self="closeModal">
      <div class="modal-content">
        <h3>Warning</h3>
        <p>Please select at least one sequence first.</p>
        <button class="modal-button" @click="closeModal">OK</button>
      </div>
    </div>

    <!-- References Section -->
    <div class="additional-info">
      <h4>References</h4>
      <blockquote>
        Uhlenbeck, O. C. &amp; Schrader, J. M. (2018). Evolutionary tuning impacts the design of bacterial tRNAs for the incorporation of unnatural amino acids by ribosomes. <em>Current Opinion in Chemical Biology</em>, 46, 138-145. doi: 10.1016/j.cbpa.2018.07.016.
      </blockquote>
      <!-- <blockquote>
        <a href="https://www.sciencedirect.com/science/article/pii/S1367593118300152" target="_blank">
          https://www.sciencedirect.com/science/article/pii/S1367593118300152
        </a>
      </blockquote> -->
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, watch, computed, nextTick } from 'vue'
import en from '@shene/table/dist/locale/en'
import { columns, pagination, onPaginationChange, type SequenceData } from './tableConfig'
import axios from 'axios'
import { aminoAcids, calculateFreeEnergy, handleAnalyzeSequence } from './computedata'

// Reactive Variables
const dataSource = ref<SequenceData[]>([])
const loading = ref(false)
const error = ref<string | null>(null)
const selectedAminoAcid = ref<number>(aminoAcids[0].deltaG) // default selection

// Row selection logic
const selectedRowKeys = ref<string[]>([])
const selectedRows = ref<SequenceData[]>([])

const rowSelection = computed(() => ({
  selectedRowKeys: selectedRowKeys.value,
  onChange: (keys: string[], rows: SequenceData[]) => {
    selectedRowKeys.value = keys
    selectedRows.value = rows
    console.log("Selection changed:", keys, rows)
  }
}))

// Load sequences and cache results
async function loadSequences() {
  const defaultSeqDataStr = localStorage.getItem('cached_sequences_after_steptwo')
  const timestamp = localStorage.getItem('timestamp_cached_sequences_after_steptwo')
  const cachedDataStr = localStorage.getItem('cached_sequences')
  const cachedTimestamp = localStorage.getItem('cached_timestamp_sequences')

  if (!defaultSeqDataStr) {
    error.value = 'No sequences found in local storage.'
    return
  }

  const parsedSequences = JSON.parse(defaultSeqDataStr) as { sequence: string }[]

  // Load cached data if valid
  if (cachedDataStr && timestamp === cachedTimestamp) {
    console.log('[INFO] Using cached sequence data.')
    dataSource.value = JSON.parse(cachedDataStr)
    return
  }

  console.log('[INFO] Cache invalid, recalculating sequence data.')

  // Initialize data
  dataSource.value = parsedSequences.map((seq, index) => ({
    key: index.toString(),
    sequence: seq.sequence,
    tstemSequence: '',
    tstemPosition: '',
    basePairs: [],
    freeEnergy: '',
    totalFreeEnergy: 0, // default value
  }))

  loading.value = true
  try {
    for (const seq of dataSource.value) {
      try {
        const response = await axios.post('/charge', { sequence: seq.sequence })
        const data = response.data
        const tstemSequence = data['T-stem Sequence'] || ''
        const { basePairs, energy } = calculateFreeEnergy(tstemSequence)

        seq.tstemSequence = tstemSequence
        seq.tstemPosition = data['T-stem Position'] || ''
        seq.basePairs = basePairs
        seq.freeEnergy = energy.toFixed(2)
        // Compute total free energy
        const parsedFreeEnergy = parseFloat(seq.freeEnergy)
        seq.totalFreeEnergy = isNaN(parsedFreeEnergy) ? 0 : calculateTotalEnergy(parsedFreeEnergy)
      } catch (e) {
        console.error(`[ERROR] Failed to process sequence: ${seq.sequence}`, e)
        seq.tstemSequence = 'The sequence quality is too poor to generate results.'
        seq.tstemPosition = 'The sequence quality is too poor to generate results.'
        seq.basePairs = []
        seq.freeEnergy = 'The sequence quality is too poor to generate results.'
        seq.totalFreeEnergy = 0 // set default on error
      }
    }

    // Cache computed results and timestamp
    localStorage.setItem('cached_sequences', JSON.stringify(dataSource.value))
    localStorage.setItem('cached_timestamp_sequences', timestamp || '')
    console.log('[INFO] Sequence data cached successfully.')
  } catch (e) {
    console.error('[ERROR] Failed to calculate sequence data:', e)
    error.value = 'Failed to calculate sequence data.'
  } finally {
    loading.value = false
  }
}

// Calculate Total Free Energy
function calculateTotalEnergy(freeEnergy: number): number {
  return freeEnergy + selectedAminoAcid.value
}

// Recalculate totalFreeEnergy on selectedAminoAcid change
watch(selectedAminoAcid, () => {
  dataSource.value.forEach(record => {
    const freeEnergy = parseFloat(record.freeEnergy)
    record.totalFreeEnergy = isNaN(freeEnergy) ? 0 : calculateTotalEnergy(freeEnergy)
  })
})

// Button Handlers
const handleSelectAll = () => {
  selectedRowKeys.value = dataSource.value.map(item => item.key)
  selectedRows.value = [...dataSource.value]
  console.log('Select All:', selectedRowKeys.value)
}

const handleClearSelection = () => {
  selectedRowKeys.value = []
  selectedRows.value = []
  console.log('Clear Selection: Selection cleared')
}

const handleSelectAbsoluteLessThanOne = () => {
  const filteredRows = dataSource.value.filter(row => Math.abs(row.totalFreeEnergy) < 1)
  selectedRowKeys.value = filteredRows.map(row => row.key)
  selectedRows.value = filteredRows
  console.log('Select Absolute Value < 1:', selectedRowKeys.value)
}

const showCustomModal = ref(false)
const closeModal = () => {
  showCustomModal.value = false
}

const handleDownloadSelected = () => {
  if (selectedRows.value.length === 0) {
    showCustomModal.value = true
    return
  }
  const headers = ['key', 'sequence', 'tstemSequence', 'tstemPosition', 'basePairs', 'freeEnergy', 'totalFreeEnergy']
  const csvContent = [
    headers.join(','),
    ...selectedRows.value.map(row =>
      `${row.key},"${row.sequence}","${row.tstemSequence}","${row.tstemPosition}","${row.basePairs.join(' ')}","${row.freeEnergy}","${row.totalFreeEnergy}"`
    )
  ].join('\n')

  const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' })
  const url = URL.createObjectURL(blob)

  const link = document.createElement('a')
  link.setAttribute('href', url)
  link.setAttribute('download', 'selected_sequences.csv')
  link.style.visibility = 'hidden'
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
}

onMounted(() => {
  loadSequences()
  nextTick(() => {
    // Optionally, automatically select all sequences
    handleSelectAll()
  })
})
</script>

<style scoped>
.title,
.description {
  text-align: center;
  margin-bottom: 20px;
}


.amino-acid-selection {
  display: flex;
  justify-content: center;
  align-items: center;
  margin: 20px 0;
}

.input-card {
  max-width: 700px;
  margin: 20px auto;
  padding: 25px;
  border-radius: 12px;
  background-color: #ffffff;
  box-shadow: 0 6px 18px rgba(0, 0, 0, 0.1);
}

.input-description {
  font-size: 15px;
  color: #606060;
  text-align: center;
  margin-bottom: 20px;
}

.collapse-container {
  border-top: 1px solid #dcdfe6;
}

.collapse-button {
  background-color: #409eff;
  color: #ffffff;
  border: none;
  padding: 8px 16px;
  border-radius: 6px;
  cursor: pointer;
  font-size: 14px;
  transition: background-color 0.3s ease;
  margin-bottom: 10px;
}

.collapse-button:hover {
  background-color: #66b1ff;
}

.input-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;
}

.button-group-inline {
  display: flex;
  gap: 10px;
  align-items: center;
}

.custom-textarea {
  white-space: pre-wrap;
  width: 100%;
  min-height: 150px;
  background-color: #f5f7fa;
  border: 2px solid #dcdfe6;
  border-radius: 8px;
  padding: 12px;
  font-size: 16px;
  line-height: 1.6;
  resize: vertical;
  transition: border-color 0.3s ease, box-shadow 0.3s ease;
  outline: none;
}

.custom-textarea:focus {
  border-color: #409eff;
  box-shadow: 0 0 8px rgba(64, 158, 255, 0.5);
}

.action-buttons {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 15px;
  margin-bottom: 20px;
}

.action-btn {
  padding: 8px 16px;
  color: #fff;
  background-color: #409eff;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  font-size: 14px;
  transition: background-color 0.3s ease, transform 0.2s ease;
}

.action-btn:hover {
  background-color: #66b1ff;
  transform: translateY(-2px);
}

.action-btn:disabled {
  background-color: #c0c4cc;
  border-color: #c0c4cc;
  cursor: not-allowed;
}

.loading,
.error {
  text-align: center;
  margin-top: 20px;
}

.error {
  color: #ff4d4f;
}

/* Modal Styles */
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background-color: rgba(0, 0, 0, 0.5);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 1000;
}

.modal-content {
  background-color: #ffffff;
  padding: 20px 30px;
  border-radius: 8px;
  text-align: center;
  max-width: 400px;
  width: 90%;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.3);
}

.modal-content h3 {
  margin-bottom: 15px;
  color: #ff4d4f;
}

.modal-content p {
  margin-bottom: 20px;
  font-size: 16px;
  color: #333333;
}

.modal-button {
  background-color: #409eff;
  color: #ffffff;
  border: none;
  padding: 8px 20px;
  border-radius: 6px;
  cursor: pointer;
  font-size: 14px;
  transition: background-color 0.3s ease;
}

.modal-button:hover {
  background-color: #66b1ff;
}

/* Additional Info / References Section */
.additional-info {
  margin-top: 40px;
  padding: 20px;
  border-top: 1px solid #dcdfe6;
  font-size: 14px;
  color: #333;
}

.additional-info h4 {
  margin-bottom: 10px;
}

.additional-info blockquote {
  margin: 10px 0;
  padding-left: 20px;
  border-left: 4px solid #409eff;
  font-style: italic;
  color: #666;
}

.custom-select select {
  padding: 8px 12px;
  font-size: 16px;
  border: 1px solid #ccc;
  border-radius: 4px;
  outline: none;
  transition: border-color 0.3s ease;
  width: 300px;
}

.custom-select select:focus {
  border-color: #409eff;
}


.label {
  margin-right: 10px;
  font-weight: bold;
  color: #333;
}

.red-button {
  background-color: red !important;
  border-color: red !important;
}

</style>
