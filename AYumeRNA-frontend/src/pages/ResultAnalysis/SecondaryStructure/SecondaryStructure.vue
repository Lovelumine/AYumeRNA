<template>
  <div>
    <h2>Secondary Structure Prediction</h2>
    <p>This section shows the predicted secondary structures.</p>
    <div v-for="(structure, index) in mockStructures" :key="index" class="structure-item">
      <p>Structure for Sequence: {{ structure.sequence }}</p>
      <p>Predicted Structure: {{ structure.predicted }}</p>
    </div>
  </div>
</template>

<script setup lang="ts">
const mockStructures = [
  { sequence: 'AUGCUAGCUAGC', predicted: 'Alpha Helix' },
  { sequence: 'GCUAGCUAGCUA', predicted: 'Beta Sheet' },
  { sequence: 'UAGCUAGCUAUG', predicted: 'Coil' },
];
</script>

<style scoped>
.structure-item {
  background: #ffffff;
  padding: 1em;
  border-radius: 8px;
  margin: 0.5em 0;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
}
</style>
