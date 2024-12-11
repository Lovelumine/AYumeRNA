<template>
  <div class="generator">
    <h2>
      <i class="fa fa-flask" aria-hidden="true"></i> Custom tRNA amino acid
      generator
    </h2>

    <!-- 引入 CodonInput 组件 -->
    <CodonInput @updateModel="updateModel" />

    <label for="sequence-count">Select Number of Sequences:</label>
    <select id="sequence-count" v-model="sequenceCount" class="select-box">
      <option v-for="count in sequenceOptions" :key="count" :value="count">
        {{ count }}
      </option>
    </select>

    <button class="generate-btn" @click="generateSequence">
      Generate Sequences
    </button>
    <p class="note">
      These generated sequences are sup-tRNA, capable of decoding stop codons.
      You can select and analyze the sequences in the table below.
    </p>

    <SequenceResult />
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import axios from 'axios' // 用于发送请求
import CodonInput from './CodonInput.vue'
import SequenceResult from './SequenceResult.vue'

const sequenceCount = ref(100)
const sequenceOptions = [10, 50, 100, 500, 1000] // 生成的数量选项
const sequences = ref<string[]>([])
const defaultReverseCodon = 'TAA' // 默认的反密码子

// 模型名称（从 CodonInput.vue 更新）
const selectedModel = ref('')

// 更新模型名称的方法
function updateModel(modelName: string) {
  console.log('Received modelName in parent:', modelName) // 检查是否收到模型名称
  selectedModel.value = modelName
}

// 保存参数到本地存储的方法
function saveParametersToLocalStorage() {
  const parameters = {
    model: selectedModel.value,
    reverseCodon: defaultReverseCodon, // 使用默认反密码子
    sequenceCount: sequenceCount.value,
  }

  console.log('Saving parameters to localStorage:', parameters)
  localStorage.setItem('generationParameters', JSON.stringify(parameters))
}

// 生成序列并发送请求
async function generateSequence() {
  try {
    // 保存当前生成参数到本地存储
    saveParametersToLocalStorage()

    // 使用 URLSearchParams 创建 x-www-form-urlencoded 格式的参数
    const params = new URLSearchParams()
    params.append('model', selectedModel.value)
    params.append('reverseCodon', defaultReverseCodon) // 使用默认反密码子
    params.append('sequenceCount', sequenceCount.value.toString())

    // 打印调试信息
    console.log('Sending request to /sample with params:', params.toString())

    // 发送 POST 请求到 /sample
    const response = await axios.post('/sample/process', params, {
      headers: {
        'Content-Type': 'application/x-www-form-urlencoded',
      },
    })

    // 从服务器响应中更新序列
    sequences.value = response.data.sequences || []
    console.log('Response:', response.data)
  } catch (error) {
    console.error('Error generating sequences:', error)
  }
}
</script>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Roboto:wght@500&family=Playfair+Display:wght@700&display=swap');

.generator {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  background: linear-gradient(to right, #eef2f3, #8e9eab);
  padding: 2em;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  width: 100%;
  margin: 0 auto;
}

h2 {
  font-family: 'Playfair Display', serif;
  color: #2c3e50;
  font-size: 2em;
  margin-bottom: 1em;
  width: 100%;
  text-align: center;
  font-weight: bold;
  padding: 0.6em 1em;
  border-radius: 8px;
  background-color: #4caf50;
  color: white;
}

h2 i {
  margin-right: 10px; /* 图标与文字的间距 */
  font-size: 1.6em; /* 图标大小 */
}

.select-box {
  margin: 1em 0;
  padding: 0.5em;
  font-size: 1em;
}

.generate-btn {
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

.generate-btn:hover {
  background-color: #45a049;
}

.note {
  font-size: 1.1em;
  color: #555;
  margin-top: 1em;
  text-align: center;
}

select {
  padding: 0.8em;
  font-size: 1em;
  border: 1px solid #ddd;
  border-radius: 4px;
  margin-top: 0.5em;
  transition: border-color 0.3s ease;
  cursor: pointer;
}

select:focus {
  border-color: #4caf50;
  outline: none;
}

label {
  font-family: 'Roboto', sans-serif;
  font-weight: 600;
  font-size: 1.3em;
  color: #333;
  margin-top: 1.5em;
}
</style>
