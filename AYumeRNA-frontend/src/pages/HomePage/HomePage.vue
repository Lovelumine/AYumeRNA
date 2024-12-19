<template>
  <div class="welcome-container">
    <h1>Welcome to iSEE-tRNA</h1>
    <p>
      iSEE-tRNA is a comprehensive Web Server designed to generate and evaluate suppressor tRNA (sup-tRNA) sequences that can decode stop codons. Our integrated platform includes a Sequence Generator powered by AI, a TRNA Evaluator for thorough assessments, and Visualization Analysis tools to display the predicted structures of your tRNA sequences.
    </p>
    <p class="highlight">
      This website is free and open to all users and there is no login requirement.
    </p>
    <button class="start-button" @click="navigateToCodonGenerator">
      Get Started
    </button>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'

const router = useRouter()

const navigateToCodonGenerator = () => {
  router.push('/CodonGenerator')
}
</script>

<style scoped>
.welcome-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100vh;
  padding: 20px;
  text-align: center;
  background-color: #f0f8ff; /* 浅色背景 */
}

h1 {
  font-size: 3em;
  margin-bottom: 20px;
  color: #333;
}

p {
  font-size: 1.2em;
  margin-bottom: 20px;
  max-width: 800px;
  color: #555;
}

.highlight {
  font-size: 1.5em;
  font-weight: bold;
  color: #d9534f; /* 红色强调 */
  margin-bottom: 30px;
}

.start-button {
  background-color: #28a745; /* 绿色背景 */
  color: white;
  border: none;
  padding: 15px 30px;
  font-size: 1.2em;
  border-radius: 8px;
  cursor: pointer;
  transition: background-color 0.3s ease;
}

.start-button:hover {
  background-color: #218838; /* 深绿色悬停效果 */
}
</style>
