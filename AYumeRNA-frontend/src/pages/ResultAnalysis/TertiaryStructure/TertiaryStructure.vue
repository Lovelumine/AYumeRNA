<template>
  <div class="tertiary-structure">
    <h2>Tertiary Structure Prediction</h2>
    <p class="description">This section shows the predicted tertiary structures of tRNA sequences. Below are some sample structures predicted based on the input sequences.</p>

    <!-- Display Demo Image -->
    <div class="demo-image">
      <img src="@/pages/ResultAnalysis/TertiaryStructure/demo.png" alt="Tertiary Structure Demo" />
    </div>

    <!-- Mock Data: Display Structures -->
    <div v-for="(structure, index) in mockStructures" :key="index" class="structure-item">
      <h3>Structure for Sequence {{ index + 1 }}</h3>
      <p><strong>Sequence:</strong> {{ structure.sequence }}</p>
      <p><strong>Predicted Tertiary Structure:</strong> {{ structure.predicted }}</p>
    </div>
  </div>
</template>

<script setup lang="ts">
const mockStructures = [
  { sequence: 'GCGTTGGTGGTATAGTGGTtAGCATAGCTGCCTTCAAAGCAGTTGaCCCGGGTTCGATTCCCGGCCAACGCACCA', predicted: 'Globular' },

];
</script>

<style scoped>
.tertiary-structure {
  padding: 30px;

  margin: 0 auto;
  font-family: 'Arial', sans-serif;
  text-align: left;
  background-color: #f9f9f9;
  border-radius: 8px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
}

h2 {
  font-size: 2.5em;
  color: #2c3e50;
  font-weight: bold;
  margin-bottom: 20px;
}

.description {
  font-size: 1.2em;
  color: #555;
  margin-bottom: 30px;
  line-height: 1.6;
}

.demo-image {
  text-align: center;
  margin-bottom: 30px;
}

.demo-image img {
  max-width: 100%;
  height: auto;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.structure-item {
  background: #ffffff;
  padding: 20px;
  border-radius: 8px;
  margin: 15px 0;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.structure-item h3 {
  font-size: 1.6em;
  color: #2c3e50;
  margin-bottom: 10px;
}

.structure-item p {
  font-size: 1.2em;
  color: #333;
  line-height: 1.6;
}

.structure-item p strong {
  font-weight: bold;
  color: #409eff;
}
</style>
