<template>
  <div class="sequence-result">
    <h3>生成的 tRNA 序列</h3>
    <p>{{ sequence }}</p>
  </div>
</template>

<script setup lang="ts">
import { defineProps } from 'vue';

const { sequence } = defineProps<{ sequence: string }>();
</script>

<style scoped>
/* 样式代码 */
</style>
