<template>
    <div class="analysis">
      <h2>生成结果与结构分析</h2>
      <SequenceResult :sequence="sequence" />
      <StructureVisualization :sequence="sequence" />
      <ScoreDisplay :score="score" />
    </div>
  </template>

  <script setup lang="ts">
  import { ref } from 'vue';
  import SequenceResult from './SequenceResult.vue';
  import StructureVisualization from './StructureVisualization.vue';
  import ScoreDisplay from './ScoreDisplay.vue';

  const sequence = ref('生成的tRNA序列');
  const score = ref(95); // 模拟评分
  </script>

  <style scoped>
  /* 样式代码 */
  </style>
