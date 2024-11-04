<template>
  <div class="analysis">
    <h2>Result Analysis</h2>
    <div v-if="selectedSequences.length">
      <h3>Selected Sequences</h3>
      <el-table :data="selectedSequences" style="width: 100%">
        <el-table-column type="selection" width="50"></el-table-column>
        <el-table-column label="Index" width="80">
          <template v-slot="scope">
            {{ scope.$index + 1 }}
          </template>
        </el-table-column>
        <el-table-column prop="sequence" label="Sequence"></el-table-column>
      </el-table>

      <button class="display-btn" @click="displaySelectedSequence">Display Selected Sequence</button>
      <button class="select-all-btn" @click="toggleSelectAll">Toggle Select All</button>
    </div>
    <router-view />
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue';

interface Sequence {
  index: number;
  sequence: string;
  selected?: boolean;
}

const selectedSequences = ref<Sequence[]>([
  { index: 1, sequence: 'AUGCUAGCUAGC', selected: false },
  { index: 2, sequence: 'GCUAGCUAGCUA', selected: false },
  { index: 3, sequence: 'UAGCUAGCUAUG', selected: false },
]);

function displaySelectedSequence() {
  const selected = selectedSequences.value.filter(seq => seq.selected);
  if (selected.length > 0) {
    alert(`Selected Sequence: ${selected.map(s => s.sequence).join(', ')}`);
  } else {
    alert('No sequences selected.');
  }
}

function toggleSelectAll() {
  const allSelected = selectedSequences.value.every(seq => seq.selected);
  selectedSequences.value.forEach(seq => {
    seq.selected = !allSelected; // 全选或全不选
  });
}
</script>

<style scoped>
.analysis {
  padding: 1.5em;
  background-color: #f9f9f9;
  border-radius: 8px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  max-width: 800px;
  margin: auto;
}

.display-btn,
.select-all-btn {
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

.display-btn:hover,
.select-all-btn:hover {
  background-color: #45a049;
}
</style>
