<template>
  <div class="sequence-result">
    <h3>Generated sup-tRNA Sequences</h3>
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
    <p class="select-note">You can select sequences for further analysis. Use the checkbox to select or deselect sequences. </p>
    <button class="analysis-btn" @click="goToAnalysis">Next Step: Analysis</button>
  </div>
</template>

<script setup lang="ts">
import { defineProps } from 'vue';
import { useRouter } from 'vue-router';
import { ElTable, ElTableColumn } from 'element-plus';

const props = defineProps<{ sequences: string[] }>();
const router = useRouter();

// 跳转到 /trex-score 路由
function goToAnalysis() {
  router.push({ name: 'TReXScore' });  // 修改为目标路由名称
}
</script>

<style scoped>
.sequence-result {
  text-align: center;
  background-color: #ffffff;
  padding: 1.5em;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  max-width: 100%;
  margin-top: 1.5em;
  width: 100%;
}

h3 {
  color: #2c3e50;
  font-size: 1.5em;
  margin-bottom: 0.5em;
}

.select-note {
  font-size: 1.1em;
  color: #555;
  margin-top: 1em;
  text-align: center;
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
