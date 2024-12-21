<template>
  <div class="sequence-analysis">
    <h2>Sequence Analysis</h2>
    <div v-if="sequences.length > 0">
      <h3>Loaded Sequences</h3>
      <ul class="sequence-list">
        <li v-for="sequence in sequences" :key="sequence.index">
          <strong>Sequence {{ sequence.index }}:</strong> {{ sequence.sequence }}
        </li>
      </ul>
    </div>
    <div v-else>
      <p>No sequences found in local storage.</p>
    </div>

    <button @click="submitSequences" class="analyze-button">Analyze Sequences</button>

    <div v-if="loading">
      <p>Loading...</p>
    </div>
    <div v-if="results">
      <h3>Download Results</h3>
      <a :href="results" download="rscape_results.txt">Download Analysis Results</a>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import axios from 'axios'

interface SequenceItem {
  index: number
  sequence: string
}

const sequences = ref<SequenceItem[]>([])
const results = ref<string>('') // 保存下载链接
const loading = ref(false)

const loadSequences = () => {
  const storedData = localStorage.getItem('sequences')
  if (storedData) {
    sequences.value = JSON.parse(storedData) as SequenceItem[]
  }
  console.log("Loaded sequences from local storage:", sequences.value)
}

const submitSequences = async () => {
  if (sequences.value.length === 0) {
    alert('No sequences to analyze!')
    return
  }

  try {
    loading.value = true
    console.log("Sequences to analyze:", sequences.value)

    // 创建 FormData 以支持文件上传
    const formData = new FormData()
    sequences.value.forEach((item, index) => {
      // 将每个序列格式化为标准的 FASTA 格式
      const fastaContent = `>seq${index + 1}\n${item.sequence}\n`
      const blob = new Blob([fastaContent], { type: 'text/plain' })
      formData.append('file', blob, `sequence_${index + 1}.fasta`)
      console.log(`Appended sequence_${index + 1}.fasta to formData`)
    })

    // 打印 FormData 内容
    for (const [key, value] of formData.entries()) {
      console.log(`FormData - ${key}:`, value)
    }

    // 发送文件到后端
    const response = await axios.post('/scape/analyze', formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
      responseType: 'blob',
    })

    console.log("Response received:", response)
    const blob = new Blob([response.data], { type: 'text/plain' })
    results.value = URL.createObjectURL(blob)
    console.log("Blob URL created:", results.value)

  } catch (error) {
    console.error('Error analyzing sequences:', error)
    alert('Error occurred while analyzing sequences.')
  } finally {
    loading.value = false
    console.log("Loading finished.")
  }
}

onMounted(loadSequences)
</script>

<style scoped>
.sequence-analysis {
  padding: 20px;
  font-family: Arial, sans-serif;
}

h2 {
  text-align: center;
  color: #333;
}

.sequence-list {
  list-style-type: none;
  padding: 0;
}

.sequence-list li {
  margin-bottom: 10px;
  font-size: 1em;
  color: #555;
}

.analyze-button {
  display: block;
  margin: 20px auto;
  padding: 10px 20px;
  font-size: 1em;
  color: #fff;
  background-color: #409eff;
  border: none;
  border-radius: 5px;
  cursor: pointer;
  transition: background 0.3s;
}

.analyze-button:hover {
  background-color: #66b1ff;
}

.results-section {
  margin-top: 20px;
  padding: 10px;
  background-color: #f9f9f9;
  border: 1px solid #ddd;
  border-radius: 5px;
  font-size: 0.9em;
  color: #333;
}
</style>
