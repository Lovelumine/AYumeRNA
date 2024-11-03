<template>
  <div class="codon-input">
    <label for="amino-acid">选择氨基酸:</label>
    <input id="amino-acid" v-model="aminoAcid" type="text" />

    <label for="species">选择物种:</label>
    <input id="species" v-model="species" type="text" />
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue';

const aminoAcid = ref('');
const species = ref('');
</script>

<style scoped>
/* 样式代码 */
</style>
