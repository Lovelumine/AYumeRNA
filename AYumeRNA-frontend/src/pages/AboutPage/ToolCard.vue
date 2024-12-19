<template>
  <el-card shadow="always" class="tool-card">
    <a :href="link" target="_blank">
      <el-image :src="imageSrc" :alt="imageAlt" class="tool-image" fit="contain"></el-image>
    </a>
    <h4>{{ title }}</h4>
    <p class="info-text">{{ description }}</p>
  </el-card>
</template>

<script lang="ts">
export default {
  name: 'ToolCard',
  props: {
    link: {
      type: String,
      required: true
    },
    imageSrc: {
      type: String,
      required: true
    },
    imageAlt: {
      type: String,
      required: true
    },
    title: {
      type: String,
      required: true
    },
    description: {
      type: String,
      required: true
    }
  }
}
</script>

<style scoped>
.tool-card {
  /* existing styles */
}
.tool-image {
  /* styles as defined earlier */
}
</style>
