<template>
    <div class="generator">
      <h2>自定义氨基酸密码子生成器</h2>
      <CodonInput />
      <button @click="generateSequence">生成序列</button>
      <SequenceResult v-if="sequence" :sequence="sequence" />
    </div>
  </template>

  <script setup lang="ts">
  import { ref } from 'vue';
  import CodonInput from './CodonInput.vue';
  import SequenceResult from './SequenceResult.vue';

  const sequence = ref('');

  function generateSequence() {
    // 模拟生成逻辑
    sequence.value = '生成的tRNA序列';
  }
  </script>

  <style scoped>
  /* 样式代码 */
  </style>
