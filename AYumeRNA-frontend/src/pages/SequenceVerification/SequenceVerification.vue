<template>
  <div class="sequence-verification-page">
    <h2>Sequence Verification</h2>
    <p>
      This page allows you to analyze the selected tRNA sequence and quickly redirect to the tRNAscan-SE web tool for further analysis.
    </p>

    <div v-if="userSequence" class="sequence-info">
      <h3>Selected tRNA Sequence for Validation</h3>
      <pre>{{ userSequence }}</pre>
    </div>

    <div v-else class="error-message">
      <p>No sequence found in local storage. Please make sure the sequence is saved in <strong>analyzedSequence</strong>.</p>
    </div>

    <button @click="copyAndRedirect" :disabled="!userSequence">
      Copy Sequence and Open tRNAscan-SE
    </button>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';

// 从本地存储读取序列
const userSequence = ref<string | null>(null);

// 页面加载时读取 localStorage 中的 analyzedSequence
onMounted(() => {
  const storedData = localStorage.getItem('analyzedSequence');
  if (storedData) {
    try {
      const parsed = JSON.parse(storedData);
      userSequence.value = parsed.sequence || null;
    } catch (error) {
      console.error('Failed to parse analyzedSequence from local storage:', error);
    }
  }
});

// 点击按钮时复制序列并跳转
const copyAndRedirect = () => {
  if (userSequence.value) {
    // 将序列复制到剪贴板
    navigator.clipboard
      .writeText(userSequence.value)
      .then(() => {
        // 打开 tRNAscan-SE 的网页
        window.open('https://lowelab.ucsc.edu/tRNAscan-SE/index.html', '_blank');
      })
      .catch((error) => {
        console.error('Failed to copy sequence to clipboard:', error);
      });
  }
};
</script>

<style scoped>
.sequence-verification-page {
  padding: 20px;
  font-family: Arial, sans-serif;
}

h2 {
  font-size: 2.2em;
  color: #2c3e50;
  margin-bottom: 1em;
}

p {
  font-size: 1.2em;
  color: #7f8c8d;
}

.sequence-info {
  background-color: #f9f9f9;
  padding: 15px;
  border-radius: 5px;
  margin-bottom: 20px;
}

pre {
  background-color: #f2f4f8;
  padding: 10px;
  border-radius: 5px;
  white-space: pre-wrap;
  word-wrap: break-word;
}

button {
  padding: 10px 20px;
  background-color: #3498db;
  color: white;
  border: none;
  border-radius: 5px;
  cursor: pointer;
  font-size: 1.1em;
  margin-top: 20px;
}

button:disabled {
  background-color: #bdc3c7;
  cursor: not-allowed;
}

button:hover:enabled {
  background-color: #2980b9;
}

.error-message {
  color: red;
  font-size: 1.1em;
  margin-top: 20px;
}
</style>
