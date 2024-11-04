<template>
  <div>
    <h2>tREX Score Analysis</h2>
    <p>This section analyzes the tREX scores for the selected sequences.</p>
    <el-table :data="mockScores" style="width: 100%">
      <el-table-column label="Sequence" prop="sequence"></el-table-column>
      <el-table-column label="tREX Score" prop="trexScore"></el-table-column>
      <el-table-column label="Action">
        <template #default="scope">
          <el-button @click="goToVisualization(scope.row.sequence)">Visualize</el-button>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router';

const mockScores = [
  { sequence: 'AUGCUAGCUAGC', trexScore: 85 },
  { sequence: 'GCUAGCUAGCUA', trexScore: 90 },
  { sequence: 'UAGCUAGCUAUG', trexScore: 78 },
];

const router = useRouter();

function goToVisualization(sequence: string) {
  router.push({ name: 'VisualizationAnalysis', query: { sequence } });
}
</script>

<style scoped>
/* 你的样式代码 */
</style>
