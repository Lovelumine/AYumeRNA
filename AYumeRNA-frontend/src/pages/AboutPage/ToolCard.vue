<!-- src/pages/AboutPage/ToolCard.vue -->
<template>
  <el-card shadow="always" class="tool-card">
    <a :href="link" target="_blank" class="tool-link">
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

<style >
.tool-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  background-color: #e3f2fd;
  border-radius: 12px;
  padding: 20px;
  max-width: 100%;  /* 限制最大宽度 */
  width: 100%;
  box-sizing: border-box; /* 使padding不影响整体宽度 */
  transition: transform 0.3s, box-shadow 0.3s;
}

.tool-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 8px 16px rgba(0, 0, 0, 0.2);
}

.tool-link {
  width: 100%;
  display: flex;
  justify-content: center;
  margin-bottom: 15px;
}

.el-image__inner {
  width: 100%;      /* 图片宽度限制为父容器宽度 */


  object-fit: contain; /* 保持图片比例 */
}


h4 {
  font-size: 18px;
  color: #0d47a1;
  margin: 10px 0;
  word-wrap: break-word; /* 允许单词换行 */
  overflow-wrap: break-word; /* 防止单词太长导致溢出 */
  word-break: break-word; /* 换行 */
}

.info-text {
  font-size: 14px;
  color: #555;
  max-width: 100%; /* 限制文字最大宽度 */
  word-wrap: break-word;
  overflow-wrap: break-word;
  text-align: center; /* 确保文字居中 */
}
</style>
