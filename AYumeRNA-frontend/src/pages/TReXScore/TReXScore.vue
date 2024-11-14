<template>
  <div class="site--main">
    <h2 class="title">tREX Score Analysis</h2>
    <p class="description">
      This section analyzes the tREX scores for the selected sequences.
      The tREX score helps to assess the quality of the sequences based on their similarity to template sequences.
    </p>

    <!-- tREX 评分公式 -->
    <div class="score-explanation">
      <h3 class="sub-title">tREX Score Formula</h3>
      <p>
        The tREX score evaluates the conservation of nucleotide positions between a test sequence and a template sequence.
        It is calculated in two steps: first by comparing the conserved positions, and then by normalizing the score to ensure comparability across sequences.
      </p>

      <!-- 公式解释 -->
      <p><strong>Step 1: Score Calculation</strong></p>
      <pre>
        tREX Score = (Conserved Positions) / (Total Positions)
      </pre>
      <p>
        The numerator represents the number of conserved positions, and the denominator represents the total number of positions being compared between the test and template sequences.
      </p>

      <p><strong>Step 2: Normalization</strong></p>
      <pre>
        Normalized tREX Score = (tREX Score - Min Score) / (Max Score - Min Score)
      </pre>
      <p>
        This normalization step ensures that all sequences' scores are within the same range, typically between 0 and 1, making it easier to compare sequences.
      </p>

      <p><strong>Note:</strong> A higher tREX score indicates a more reliable sequence that closely matches the template sequence.
      </p>
    </div>

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
