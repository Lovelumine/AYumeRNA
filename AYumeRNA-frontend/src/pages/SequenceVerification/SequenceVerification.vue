<template>
  <div class="sequence-verification-page">
    <h2>Sequence Verification</h2>
    <p>
      This page allows you to compare a generated tRNA sequence with a natural tRNA sequence using the tRNAscan-SE tool for verification.
    </p>

    <div class="sequence-info">
      <h3>Selected tRNA Sequence for Validation</h3>
      <pre>{{ userSequence }}</pre>
    </div>

    <button @click="validateSequence">Start Sequence Validation</button>

    <div v-if="validationResult" class="result-section">
      <h3>Validation Result</h3>
      <p><strong>Matching Score:</strong> {{ validationResult.matchingScore }}</p>
      <p><strong>Match Status:</strong> {{ validationResult.matchStatus }}</p>

      <h4>Aligned Sequences</h4>
      <div class="aligned-sequences">
        <div class="sequence">
          <strong>Generated tRNA Sequence</strong>
          <pre :style="{ whiteSpace: 'pre-wrap' }" v-html="generateAlignedSequence(userSequence, validationResult.matchedRegions)"></pre>
        </div>
        <div class="sequence">
          <strong>Natural tRNA Sequence</strong>
          <pre :style="{ whiteSpace: 'pre-wrap' }" v-html="generateAlignedSequence(validationResult.naturalSequence, validationResult.matchedRegions)"></pre>
        </div>
      </div>

      <h4>Matched Regions</h4>
      <ul>
        <li v-for="(region, index) in validationResult.matchedRegions" :key="index">
          Start: {{ region.start }} - End: {{ region.end }} (Length: {{ region.length }})
        </li>
      </ul>
    </div>

    <div v-if="errorMessage" class="error-message">
      <p>{{ errorMessage }}</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue';

// 固定的用户tRNA序列，来自Python生成的序列
const userSequence = ref(
  'CCTTGTTTCCGGGAAACCTACCAGGGTGACTTGCTCGAGTCCGCAAAAAA'  // 生成的 tRNA 序列
);

// 错误信息
const errorMessage = ref('');

// 定义 validationResult 类型
interface ValidationResult {
  matchingScore: string;
  matchStatus: string;
  matchedRegions: { start: number; end: number; length: number }[];
  naturalSequence: string;
}

// 验证结果
const validationResult = ref<ValidationResult | null>(null);

// 模拟天然 tRNA 序列，用于比对
const naturalSequence = 'CCTTGTTTCCGCTCTACCTACCAGGGTTAATTGCTCGAGTCCGCAAAACT';

// 验证 tRNA 序列的函数
const validateSequence = () => {
  // 模拟 tRNAscan-SE 工具返回的验证结果
  setTimeout(() => {
    // 计算匹配分数（这里用简单的字符串相似度来模拟）
    const similarity = calculateSequenceSimilarity(userSequence.value, naturalSequence);
    validationResult.value = {
      matchingScore: similarity.toFixed(2),  // 结果保留两位小数
      matchStatus: similarity > 0.8 ? 'High match' : 'Low match',
      matchedRegions: [
        { start: 0, end: 9, length: 10 },
        { start: 15, end: 24, length: 10 },
        { start: 30, end: 40, length: 11 },
      ],
      naturalSequence: naturalSequence,
    };
    errorMessage.value = ''; // 清除错误信息
  }, 1000); // 模拟异步请求
};

// 计算两个 tRNA 序列的相似度（简单示例）
const calculateSequenceSimilarity = (seq1: string, seq2: string) => {
  const minLength = Math.min(seq1.length, seq2.length);
  let matches = 0;
  for (let i = 0; i < minLength; i++) {
    if (seq1[i] === seq2[i]) {
      matches++;
    }
  }
  return matches / minLength;  // 返回相似度分数
};

// 生成平行显示的序列，带有颜色标记
const generateAlignedSequence = (sequence: string, matchedRegions: { start: number; end: number }[]) => {
  let alignedSequence = '';
  let lastIndex = 0;

  // 循环匹配的区域并加上颜色
  matchedRegions.forEach(region => {
    const beforeMatch = sequence.slice(lastIndex, region.start);
    const match = sequence.slice(region.start, region.end + 1);
    const afterMatch = sequence.slice(region.end + 1);

    alignedSequence += `<span style="color: gray;">${beforeMatch}</span>`; // 不匹配部分
    alignedSequence += `<span style="color: green; background-color: #d4f4d4;">${match}</span>`; // 匹配部分
    alignedSequence += `<span style="color: gray;">${afterMatch}</span>`; // 不匹配部分

    lastIndex = region.end + 1;
  });

  return alignedSequence;
};
</script>

<style scoped>
.sequence-verification-page {
  padding: 20px;
  font-family: Arial, sans-serif;
}

h2 {
  font-size: 2.2em;
  color: #2c3e50;
  margin-bottom: 1em;
}

h3 {
  font-size: 1.8em;
  color: #34495e;
  margin-top: 1.5em;
}

p {
  font-size: 1.2em;
  color: #7f8c8d;
}

.sequence-info {
  background-color: #f9f9f9;
  padding: 15px;
  border-radius: 5px;
  margin-bottom: 20px;
}

pre {
  background-color: #f2f4f8;
  padding: 10px;
  border-radius: 5px;
  white-space: pre-wrap;
  word-wrap: break-word;
}

button {
  padding: 10px 20px;
  background-color: #3498db;
  color: white;
  border: none;
  border-radius: 5px;
  cursor: pointer;
  font-size: 1.1em;
  margin-top: 20px;
}

button:hover {
  background-color: #2980b9;
}

.result-section {
  margin-top: 30px;
  background-color: #f9f9f9;
  padding: 20px;
  border-radius: 5px;
  box-shadow: 0 2px 5px rgba(0, 0, 0, 0.1);
}

ul {
  list-style-type: none;
  padding-left: 0;
}

ul li {
  background-color: #ecf0f1;
  padding: 8px;
  margin: 5px 0;
  border-radius: 5px;
}

.error-message {
  color: red;
  margin-top: 20px;
}

.aligned-sequences {
  display: flex;
  justify-content: space-between;
  margin-top: 20px;
}

.sequence {
  width: 45%;
}
</style>
