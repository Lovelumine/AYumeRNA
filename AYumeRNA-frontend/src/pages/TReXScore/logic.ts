// src/pages/TReXScore/logic.ts
import axios from 'axios'
import { ref } from 'vue'

export type Sequence = {
  sequence: string
  trexScore: number | null
}

export type GenerationParameters = {
  model: string
  sequenceCount: number
}

export const lastSubmittedCodon = ref<string | null>(null)

export function getCodonStorageKey(codon: string): string {
  return `sequences_${codon}`
}

export function getCodonTimestampKey(codon: string): string {
  return `timestamp_${codon}`
}

export function sequencesToFA(seqArr: Sequence[]): string {
  return seqArr
    .map((seq, index) => `>seq${index + 1}\n${seq.sequence}`)
    .join('\n')
}

export function loadDefaultSequences(sequences: {
  value: Sequence[]
}): boolean {
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

export async function fetchAndReplaceSequences(
  url: string,
  sequences: { value: Sequence[] },
): Promise<void> {
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
    if (currentTimestampSequences && lastSubmittedCodon.value) {
      // 只更新本次提交对应的codon数据
      localStorage.setItem(
        getCodonStorageKey(lastSubmittedCodon.value),
        JSON.stringify(newSequences),
      )
      localStorage.setItem(
        getCodonTimestampKey(lastSubmittedCodon.value),
        currentTimestampSequences,
      )
      console.log(
        'Updated codon scored sequences and timestamp for',
        lastSubmittedCodon.value,
      )
      lastSubmittedCodon.value = null
    }
  } catch (error) {
    console.error('Error fetching or replacing sequences:', error)
  }
}

export function submitTask(
  aminoAcid: string,
  domain: string,
  selectedReverseCodon: string,
  sequences: { value: Sequence[] },
  wsMessages: { value: string[] },
  taskSubmitted: { value: boolean },
  connectWebSocket: () => void,
): void {
  console.log('submitTask called.')
  if (sequences.value.length === 0) {
    console.log('No sequences available, no submit.')
    return
  }

  lastSubmittedCodon.value = selectedReverseCodon

  const formData = new FormData()
  formData.append('aminoAcid', aminoAcid)
  formData.append('domain', domain)
  formData.append('reverseCodon', selectedReverseCodon)
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
      connectWebSocket() // 确保在提交后连接WebSocket
    })
    .catch(error => {
      console.error('Error submitting task:', error)
    })
}
