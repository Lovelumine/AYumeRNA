<!-- src/pages/TRNAEvaluator/StructureFoldingEvaluation/StructureFoldingEvaluation.vue -->
<template>
  <div class="structure-folding-evaluation">
    <h3 class="title">Structure Folding Evaluation</h3>
    <p class="description">
      The table below displays the structure folding parameters of each sequence, including anticodon, infernal score, and tRNA positions.
      These parameters are key to understanding the structural features of the tRNA.
    </p>

    <!-- 使用 shane 表格展示 -->
    <s-table-provider :locale="en">
      <s-table
        :columns="columns"
        :data-source="dataSource"
        :max-height="300"
        row-key="key"
      >
        <template #bodyCell="{ text, column, record }">
          <template v-if="column.key === 'sequence'">
            <a>{{ text }}</a>
          </template>
          <template v-else-if="column.key === 'anticodon'">
            {{ record.anticodon }}
          </template>
          <template v-else-if="column.key === 'infernalScore'">
            {{ record.infernalScore }}
          </template>
          <template v-else-if="column.key === 'tRNAStart'">
            {{ record.tRNAStart }}
          </template>
          <template v-else-if="column.key === 'tRNAEnd'">
            {{ record.tRNAEnd }}
          </template>
          <template v-else-if="column.key === 'tRNAType'">
            {{ record.tRNAType }}
          </template>
          <template v-else-if="column.key === 'actions'">
            <button class="action-btn" @click="handleAnalyzeSequence(record)">
              Visualization
            </button>
          </template>
        </template>
      </s-table>
    </s-table-provider>

    <!-- 加载和错误提示 -->
    <div v-if="loading" class="loading">Loading...</div>
    <div v-if="error" class="error">{{ error }}</div>
  </div>
</template>

<script setup lang="ts">
import { onMounted } from 'vue'
import en from '@shene/table/dist/locale/en'
import { columns } from './tableConfig'
import {
  dataSource,
  loading,
  error,
  loadSequences,
  handleAnalyzeSequence
} from './logic'

// 在 mounted 时加载序列并开始请求
onMounted(() => {
  loadSequences()
})
</script>

<style scoped>
.title, .description {
  text-align: center;
}

.loading, .error {
  text-align: center;
  margin-top: 20px;
}

.error {
  color: red;
}

.loading {
  color: #409eff;
}

.action-btn {
  padding: 4px 8px;
  color: #fff;
  background-color: #409eff;
  border: none;
  border-radius: 4px;
  cursor: pointer;
}

.action-btn:hover {
  background-color: #66b1ff;
}
</style>
