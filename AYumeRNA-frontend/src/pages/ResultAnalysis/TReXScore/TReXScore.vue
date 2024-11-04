<template>
  <div>
    <h2>tREX Score Analysis</h2>
    <p>This section analyzes the tREX scores for the selected sequences.</p>
    <div v-for="(score, index) in mockScores" :key="index" class="score-item">
      <p>Sequence: {{ score.sequence }}</p>
      <p>tREX Score: {{ score.trexScore }}</p>
    </div>
  </div>
</template>

<script setup lang="ts">
const mockScores = [
  { sequence: 'AUGCUAGCUAGC', trexScore: 85 },
  { sequence: 'GCUAGCUAGCUA', trexScore: 90 },
  { sequence: 'UAGCUAGCUAUG', trexScore: 78 },
];
</script>

<style scoped>
.score-item {
  background: #ffffff;
  padding: 1em;
  border-radius: 8px;
  margin: 0.5em 0;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
}
</style>
