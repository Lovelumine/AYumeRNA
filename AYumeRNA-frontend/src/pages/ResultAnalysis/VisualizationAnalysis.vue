<template>
  <div class="visualization-analysis">
    <h2>tRNA Visualization Analysis</h2>
    <p>In this section, you can view the predicted secondary and tertiary structures of tRNA sequences.</p>

    <p><strong>Currently analyzing the following tRNA sequence:</strong></p>
    <pre>{{ tRNASequence }}</pre>

    <!-- Buttons for navigating to sub-pages -->
    <div class="sub-pages">
      <router-link to="/visualization/secondary-structure">
        <button class="nav-btn secondary">View Secondary Structure Prediction (using R2DT)</button>
      </router-link>
      <router-link to="/visualization/tertiary-structure">
        <button class="nav-btn tertiary">View Tertiary Structure Prediction (using AlphaFold 3)</button>
      </router-link>
    </div>

    <!-- Render sub-routes -->
    <router-view></router-view>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'

import { useRouter, useRoute } from 'vue-router'

const router = useRouter()
const route = useRoute()


// Define tRNASequence as a reactive reference
const tRNASequence = ref('')

// Fetch the analyzed sequence from local storage when the component is mounted
onMounted(() => {
  const analyzedData = localStorage.getItem('analyzedSequence')
  if (analyzedData) {
    try {
      const parsedData = JSON.parse(analyzedData)
      tRNASequence.value = parsedData.sequence || 'No sequence available'
    } catch (error) {
      console.error('Failed to parse analyzedSequence from local storage:', error)
      tRNASequence.value = 'Error retrieving sequence'
    }
  } else {
    tRNASequence.value = 'No analyzed sequence found in local storage'
  }
})

  // 自动跳转到 /visualization/secondary-structure 子页面
  if (route.path === '/visualization') {
    router.push('/visualization/secondary-structure')
  }

</script>

<style scoped>
.visualization-analysis {
  padding: 30px;
  margin: 0 auto;
  font-family: 'Arial', sans-serif;
}

h2 {
  font-size: 2.5em;
  color: #2c3e50;
  font-weight: bold;
  text-align: center;
  margin-bottom: 20px;
}

p {
  font-size: 1.2em;
  color: #333;
  line-height: 1.6;
  text-align: center;
}

strong {
  font-weight: bold;
  color: #409eff;
}

pre {
  background-color: #f4f4f4;
  padding: 12px;
  border-radius: 6px;
  white-space: pre-wrap;
  word-wrap: break-word;
  font-size: 1.1em;
  color: #333;
  margin: 20px auto;
  width: 100%;
  max-width: 800px;
}

.sub-pages {
  margin-top: 30px;
  text-align: center;
}

.nav-btn {
  background-color: #409eff;
  color: white;
  font-size: 1.1em;
  padding: 12px 20px;
  border-radius: 6px;
  text-align: center;
  text-decoration: none;
  display: inline-block;
  transition: all 0.3s ease;
  margin: 10px 0;
}

.nav-btn:hover {
  background-color: #66b1ff;
  transform: scale(1.05);
}

.nav-btn.secondary {
  background-color: #42b983;
}

.nav-btn.secondary:hover {
  background-color: #61c28d;
}

.nav-btn.tertiary {
  background-color: #f39c12;
}

.nav-btn.tertiary:hover {
  background-color: #f1a10b;
}
</style>
