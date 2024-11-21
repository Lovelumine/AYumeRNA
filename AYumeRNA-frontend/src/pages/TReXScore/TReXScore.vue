<template>
  <div class="site--main">
    <h2 class="title">tREX Score Analysis</h2>
    <p class="description">
      This section analyzes the tREX scores for the selected sequences.
      The tREX score helps to assess the quality of the sequences based on their similarity to template sequences.
    </p>

    <!-- 使用 TableWithAction 组件 -->
    <TableWithAction :data-source="sequences" />
  </div>
</template>

<script setup lang="ts">
import TableWithAction from './TableWithAction'; // 引入 TableWithAction 组件
import { ref, onMounted } from 'vue';

// 用于存储从 localStorage 读取的序列数据
const sequences = ref<{ sequence: string; trexScore: number | null }[]>([]);

// 从 localStorage 读取数据并赋值给 sequences
onMounted(() => {
  console.log('Attempting to load sequences from localStorage...');

  // 尝试读取 localStorage 中的数据
  const savedSequences = localStorage.getItem('sequences');
  console.log('Saved sequences from localStorage:', savedSequences);

  if (savedSequences) {
    try {
      const parsedSequences = JSON.parse(savedSequences);
      console.log('Parsed sequences:', parsedSequences);

      // 假设本地存储的数据已经是符合结构的
      sequences.value = parsedSequences.map((seq: { sequence: string }) => ({
        sequence: seq.sequence,
        trexScore: null, // 初始时不计算分数，可以在后续更新
      }));

      console.log('Sequences loaded and mapped:', sequences.value);
    } catch (error) {
      console.error('Error parsing sequences from localStorage:', error);
    }
  } else {
    console.warn('No sequences found in localStorage.');
  }
});
</script>

<style scoped>
.site--main {
  padding: 20px;
}

.title {
  font-size: 1.8em;
  color: #2c3e50;
  margin-bottom: 1em;
  font-weight: bold;
}

.description {
  font-size: 1.2em;
  color: #7f8c8d;
  margin-bottom: 1.5em;
}

.sub-title {
  font-size: 1.5em;
  color: #2c3e50;
  margin-bottom: 1em;
}

.score-explanation {
  margin-bottom: 2em;
  font-size: 1.1em;
  color: #7f8c8d;
}

.score-explanation pre {
  background-color: #f4f4f4;
  padding: 10px;
  border-radius: 5px;
  font-size: 1em;
  color: #333;
  margin: 10px 0;
}

.s-table {
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.s-table th {
  background-color: #f2f4f8;
  color: #2c3e50;
  font-weight: bold;
}

.s-table .s-table__row {
  border-bottom: 1px solid #e0e4e7;
}

.link {
  color: #409eff;
  cursor: pointer;
}

.link:hover {
  text-decoration: underline;
}

.sequence-column {
  font-weight: bold;
  color: #2c3e50;
}

.trex-score-column {
  color: #3498db;
}

.action-column {
  text-align: center;
}
</style>
