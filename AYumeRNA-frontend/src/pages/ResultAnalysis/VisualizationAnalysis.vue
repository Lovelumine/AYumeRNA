<template>
  <div class="container">
    <!-- 文字描述 -->
    <p class="text-description">
       tRNA sequence:
      <span class="sequence">
        GCGUUGGUGGUAUAGUGGUUAGCAUAGCUGCCUUCAAAGCAGUUGACCCGGGUUCGAUUCCCGGCCAACGCA
      </span>
    </p>
    <!-- 图片 -->
    <img src="./demo.png" alt="Demo Image" class="image" />
  </div>
</template>

<script setup lang="ts">
// 这里可以添加 TypeScript 代码逻辑
</script>

<style scoped>
/* 使用 flexbox 来居中内容 */
.container {
  display: flex;
  flex-direction: column;  /* 使内容垂直排列 */
  justify-content: center;
  align-items: center;
  height: 100vh; /* 使页面高度为视口的高度 */
  margin: 0; /* 移除默认的页面边距 */
  background-color: #f0f0f0; /* 设置背景颜色为淡灰色 */
  text-align: center; /* 让文字居中 */
}

.text-description {
  font-size: 1.2em; /* 增加文字的大小 */
  color: #333; /* 设置文字颜色 */
  margin-bottom: 20px; /* 增加文字与图片的间距 */
}

.sequence {
  font-weight: bold;  /* 让序列加粗 */
  color: #0077cc; /* 让序列文字颜色为蓝色 */
}

.image {
  max-width: 90%;  /* 控制图片最大宽度 */
  max-height: 90%; /* 控制图片最大高度 */
  object-fit: contain; /* 保持图片比例 */
}
</style>
