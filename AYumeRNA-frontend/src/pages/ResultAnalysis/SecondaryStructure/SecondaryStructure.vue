<template>
  <div class="secondary-structure">
    <h2>tRNA Secondary Structure Prediction</h2>

    <!-- Loading indicator -->
    <div v-if="loading" class="loading">
      <p>Loading prediction results...</p>
    </div>

    <!-- Displaying the results -->
    <div v-if="predictionResult">
      <h3>Dot-Bracket Structure</h3>
      <pre>{{ predictionResult.dot_bracket }}</pre>

      <h3>SVG Visualizations</h3>
      <div class="forna-container">
        <div class="svg-view">
          <h4>Colored SVG</h4>
          <p>This image shows the colored secondary structure of tRNA, helping to identify different structural regions.</p>
          <div v-html="predictionResult.svg_files.colored_svg"></div>
        </div>
        <div class="svg-view">
          <h4>Enriched SVG</h4>
          <p>This image provides an enriched view of the tRNA secondary structure, including detailed annotations and information.</p>
          <div v-html="predictionResult.svg_files.enriched_svg"></div>
        </div>
        <div class="svg-view">
          <h4>Thumbnail SVG</h4>
          <p>This image is a thumbnail of the tRNA secondary structure, suitable for quick preview.</p>
          <div v-html="predictionResult.svg_files.thumbnail_svg"></div>
        </div>
      </div>
    </div>

    <!-- Error handling -->
    <div v-if="error" class="error-message">
      <p>Error: {{ error }}</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import axios from 'axios'

interface PredictionResult {
  dot_bracket: string
  svg_files: {
    colored_svg: string
    enriched_svg: string
    thumbnail_svg: string
  }
}

// State management
const rnaSequence = ref<string | null>(null)
const predictionResult = ref<PredictionResult | null>(null)
const loading = ref(false)
const error = ref<string | null>(null)

// Fetch sequence from local storage and request prediction
onMounted(async () => {
  // Load sequence from local storage
  const storedSequence = localStorage.getItem('analyzedSequence')
  if (storedSequence) {
    try {
      const parsed = JSON.parse(storedSequence)
      rnaSequence.value = parsed.sequence
    } catch (e) {
      console.error('Error parsing sequence from local storage:', e)
      error.value = 'Invalid sequence data in local storage'
      return
    }
  } else {
    error.value = 'No sequence found in local storage'
    return
  }

  // Request prediction from R2DT
  if (rnaSequence.value) {
    try {
      loading.value = true
      const response = await axios.post('/r2dt/run', {
        sequence: rnaSequence.value,
      })
      predictionResult.value = response.data
    } catch (e) {
      console.error('Error fetching R2DT results:', e)
      error.value = 'Failed to fetch prediction results'
    } finally {
      loading.value = false
    }
  }
})
</script>

<style scoped>
.secondary-structure {
  padding: 30px;
  width: 100%;
  margin: 0 auto;
  font-family: 'Arial', sans-serif;
  text-align: left;
}

h2 {
  font-size: 2.5em;
  color: #2c3e50;
  font-weight: bold;
  margin-bottom: 20px;
}

h3 {
  font-size: 1.6em;
  color: #333;
  margin-bottom: 10px;
}

h4 {
  font-size: 1.4em;
  color: #409eff;
  margin-bottom: 10px;
}

pre {
  background-color: #f4f4f4;
  padding: 12px;
  border-radius: 6px;
  white-space: pre-wrap;
  word-wrap: break-word;
  font-size: 1.1em;
  color: #333;
  margin: 20px 0;
  width: 100%;
}

.loading {
  text-align: center;
  font-size: 1.2em;
  color: #888;
}

.error-message {
  text-align: center;
  font-size: 1.2em;
  color: red;
}

.forna-container {
  margin-top: 30px;
  padding: 20px;
  background-color: #f9f9f9;
  border-radius: 8px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
}

.svg-view {
  margin-top: 20px;
  padding: 15px;
  border: 1px solid #ddd;
  border-radius: 6px;
  background-color: #fff;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.05);
  text-align: center; /* Center content */
}

.svg-view h4 {
  margin-bottom: 10px;
}

.svg-view p {
  margin-bottom: 15px;
  font-size: 1em;
  color: #666;
}

.svg-view > div {
  overflow-x: auto;
  display: inline-block; /* Ensure content is centered */
}
</style>
