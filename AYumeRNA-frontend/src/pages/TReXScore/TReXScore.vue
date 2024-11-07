<template>
  <div class="site--main">
    <h2 class="title">tREX Score Analysis</h2>
    <p class="description">This section analyzes the tREX scores for the selected sequences.</p>

    <!-- 使用 TableWithAction 组件 -->
    <TableWithAction :data-source="mockScores" />
  </div>
</template>

<script setup lang="ts">
import TableWithAction from './TableWithAction'; // 引入 TableWithAction 组件

const mockScores = [
  { sequence: 'AUGGCUACGUGCGCUACG', trexScore: 0.85 },
  { sequence: 'GGCUAGGCUAUGCAGCGA', trexScore: 0.45 },
  { sequence: 'UUAGCUAGGCUAGGUCUA', trexScore: -0.23 },
  { sequence: 'ACGUGCCUAGCUAGCUUA', trexScore: 0.78 },
  { sequence: 'UAGGCUAGCUAUGCCAGC', trexScore: 0.12 },
  { sequence: 'UAGCUAGCUUAGCGUAGC', trexScore: -0.56 },
  { sequence: 'AACGUCUAGCUAUGGCAA', trexScore: 0.91 },
  { sequence: 'GCAUCGUGGCUAGCUCGG', trexScore: -0.34 },
  { sequence: 'UAGCAGUAGCGCGAGGCA', trexScore: 0.33 },
  { sequence: 'UCUAGCGAUAGCCGACUG', trexScore: 0.67 },
];
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
