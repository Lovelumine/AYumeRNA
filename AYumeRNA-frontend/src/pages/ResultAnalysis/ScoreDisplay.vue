<template>
  <div class="score-display">
    <h3>评分: {{ props.score }}</h3>
  </div>
</template>

<script setup lang="ts">
import { defineProps } from 'vue';

const props = defineProps<{ score: number }>();
</script>

<style scoped>
/* 样式代码 */
</style>
