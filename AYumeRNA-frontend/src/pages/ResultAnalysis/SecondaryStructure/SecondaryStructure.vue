<template>
  <div class="secondary-structure">
    <h2>tRNA Secondary Structure Prediction</h2>

    <!-- tRNA 相关信息 -->
    <section class="info-section">
      <h3>tRNA Information</h3>
      <table>
        <tr>
          <th>Name</th>
          <td>{{ tRNAInfo.name }}</td>
        </tr>
        <tr>
          <th>Sequence</th>
          <td>{{ tRNAInfo.sequence }}</td>
        </tr>
        <tr>
          <th>Type</th>
          <td>{{ tRNAInfo.type }}</td>
        </tr>
        <tr>
          <th>Anticodon</th>
          <td>{{ tRNAInfo.anticodon }}</td>
        </tr>
        <tr>
          <th>Score</th>
          <td>{{ tRNAInfo.score }}</td>
        </tr>
        <tr>
          <th>Intron Bounds</th>
          <td>{{ tRNAInfo.intronBounds }}</td>
        </tr>
      </table>
    </section>

    <!-- Forna 二级结构展示 -->
    <section class="forna-container">
      <h3>tRNA Secondary Structure Visualization</h3>
      <div id="forna" class="forna-view"></div>
    </section>

    <!-- 序列展示 -->
    <section class="sequence-section">
      <h3>tRNA Sequence</h3>
      <pre>{{ tRNAInfo.sequence }}</pre>
    </section>

    <!-- 下载链接 -->
    <section class="download-section">
      <button class="download-btn" @click="downloadSequence">Download tRNA Sequence (FASTA)</button>
    </section>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import * as fornac from '@/fornac/fornac.js';  // 导入整个 fornac 模块

// Mock 数据：tRNA 信息
const tRNAInfo = ref({
  name: "seq1",
  sequence: "CGCUUCAUAUAAUCCUAAUGAUAUGGUUUGGGAGUUUCUACCAAGAGCCUUAAACUCUUGAUUAUGAAGUG",
  type: "Sup",
  anticodon: "TCA (34-36)",
  score: 73.8,
  intronBounds: "0-0"
});

// Forna 二级结构渲染
onMounted(() => {
  const fornaContainer = document.getElementById('forna');
  if (fornaContainer) {
    const structure = '....(((((((..((((((.........))))))......).((((((.......))))))..))))))...';  // 假设这是一个有效的结构
    const sequence = tRNAInfo.value.sequence;

    // 创建 Forna 实例并绘制结构
    const forna = new fornac.FornaContainer(fornaContainer, {
      animation: true,
      zoomable: true,
      editable: false,
    });

    // 使用正确的 structure 和 sequence 进行展示
    forna.addRNA(sequence, structure);
    forna.displayNumbering(false);
  }
});


// 下载 FASTA 序列
const downloadSequence = () => {
  const fastaContent = `>${tRNAInfo.value.name}\n${tRNAInfo.value.sequence}`;
  const blob = new Blob([fastaContent], { type: 'text/plain' });
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = `${tRNAInfo.value.name}.fasta`;
  a.click();
  URL.revokeObjectURL(url);  // 清理 URL 对象
};
</script>

<style scoped>
.secondary-structure {
  padding: 30px;
  max-width: 900px;
  margin: 0 auto;
  font-family: 'Arial', sans-serif;
}

h2 {
  font-size: 2.5em;
  color: #2c3e50;
  font-weight: bold;
  text-align: center;
  margin-bottom: 20px;
}

h3 {
  font-size: 1.6em;
  color: #333;
  margin-bottom: 10px;
}

table {
  width: 100%;
  border-collapse: collapse;
  margin-bottom: 30px;
}

th, td {
  padding: 10px;
  text-align: left;
  border: 1px solid #ddd;
}

th {
  background-color: #f4f4f4;
  font-weight: bold;
}

td {
  color: #333;
}

pre {
  background-color: #f4f4f4;
  padding: 12px;
  border-radius: 6px;
  white-space: pre-wrap;
  word-wrap: break-word;
  font-size: 1.1em;
  color: #333;
  margin: 20px auto;
  width: 100%;
  max-width: 800px;
}

.forna-container {
  margin-top: 30px;
  padding: 20px;
  background-color: #f9f9f9;
  border-radius: 8px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
  text-align: center;
}

.forna-view {
  margin-top: 20px;
  height: 400px; /* Set height for Forna display */
  border: 1px solid #ddd;
  background-color: #fff;
}

.download-btn {
  background-color: #409eff;
  color: white;
  padding: 12px 20px;
  border-radius: 6px;
  font-size: 1.2em;
  cursor: pointer;
  transition: all 0.3s ease;
  border: none;
}

.download-btn:hover {
  background-color: #66b1ff;
  transform: scale(1.05);
}
</style>
