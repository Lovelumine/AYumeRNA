<template>
  <div class="sequence-result">
    <h3>Generated tRNA Sequences</h3>
    <el-table :data="props.sequences" style="width: 100%">
      <el-table-column type="selection" width="50"></el-table-column>
      <el-table-column prop="index" label="Index" width="80">
        <template #default="scope">
          {{ scope.$index + 1 }}
        </template>
      </el-table-column>
      <el-table-column prop="sequence" label="Sequence">
        <template #default="scope">
          {{ scope.row }}
        </template>
      </el-table-column>
    </el-table>
    <button class="analysis-btn" @click="goToAnalysis">Next Step: Analysis</button>
  </div>
</template>

<script setup lang="ts">
import { defineProps } from 'vue';
import { useRouter } from 'vue-router';
import { ElTable, ElTableColumn } from 'element-plus';

const props = defineProps<{ sequences: string[] }>();
const router = useRouter();

function goToAnalysis() {
  router.push({ name: 'ResultAnalysis' });
}
</script>

<style scoped>
.sequence-result {
  text-align: center;
  background-color: #ffffff;
  padding: 1.5em;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  max-width: 800px;
  margin-top: 1.5em;
  max-height: 400px; /* 限制最大高度 */
  overflow-y: auto;  /* 启用垂直滚动 */
}

h3 {
  color: #2c3e50;
  font-size: 1.5em;
  margin-bottom: 0.5em;
}

.analysis-btn {
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

.analysis-btn:hover {
  background-color: #45a049;
}
</style>
