<template>
  <div class="generator">
    <h2>Custom Amino Acid Codon Generator</h2>
    <CodonInput />
    <label for="sequence-count">Select Number of Sequences:</label>
    <select id="sequence-count" v-model="sequenceCount" class="select-box">
      <option v-for="count in sequenceOptions" :key="count" :value="count">{{ count }}</option>
    </select>
    <button class="generate-btn" @click="generateSequence">Generate Sequences</button>
    <SequenceResult v-if="sequences.length" :sequences="sequences" />
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import CodonInput from './CodonInput.vue';
import SequenceResult from './SequenceResult.vue';

const sequenceCount = ref(100);
const sequenceOptions = [10, 50, 100, 500, 1000]; // 生成的数量选项
const sequences = ref<string[]>([]);

function generateSequence() {
  sequences.value = Array.from({ length: sequenceCount.value }, (_, i) => `Sequence ${i + 1}`);
}
</script>

<style scoped>
.generator {
  display: flex;
  flex-direction: column;
  align-items: center;
  background: linear-gradient(to right, #eef2f3, #8e9eab);
  padding: 2em;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  max-width: 600px;
  margin: auto;
}

h2 {
  color: #2c3e50;
  font-size: 1.8em;
  margin-bottom: 1em;
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
</style>
