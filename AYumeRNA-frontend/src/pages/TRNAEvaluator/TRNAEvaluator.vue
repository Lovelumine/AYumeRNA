<template>
  <div class="site--main">
    <h2 class="trna-evaluator">tRNA Evaluator</h2>
    <div class="info-box">
      <h3>Overview</h3>
      <p>
        Welcome to the <strong>tRNA Evaluator</strong>. In the first step, sup-tRNA sequences with special suppressor capabilities were generated using AI. Now, in this second phase, we evaluate these tRNAs and determine if they can carry specific amino acids and read through stop codons.
      </p>

      <details class="details-box">
        <summary class="details-summary">
          Show More About the First Step
        </summary>
        <div class="details-content">
          <h4>Step 1: Generating sup-tRNA Sequences</h4>
          <p>
            In the first step, computational models and reference datasets were used to generate new sup-tRNA sequences with potential stop codon suppression capabilities. At this stage, the specific amino acids they carry were not yet determined. These sequences serve as candidates for further evaluation in this phase.
          </p>
        </div>
      </details>
    </div>

    <div class="buttons">

      <router-link to="/trna-evaluator/structure-folding" active-class="active-btn">
        <button class="btn">Structure Folding Evaluation</button>
      </router-link>
      <router-link to="/trna-evaluator/identity-elements" active-class="active-btn">
        <button class="btn">Identity Elements Evaluation</button>
      </router-link>
      <router-link to="/trna-evaluator/aminoacylation" active-class="active-btn">
        <button class="btn">The affinity between aa-tRNAs and EF-Tu</button>
      </router-link>
    </div>

    <!-- 子路由渲染 -->
    <router-view></router-view>
  </div>
</template>

<script setup lang="ts">
</script>

<style scoped>

.site--main {
  padding-right: 20px;
  font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
  line-height: 1.6;

  margin: 0 auto;
}

.description {
  font-size: 1.2em;
  text-align: center;
  margin-bottom: 30px;
  color: #555;
}

.trna-evaluator {
  text-align: center;
  margin: 20px;
}

.buttons {
  text-align: center;
  margin: 20px 0;
}

.btn {
  margin: 10px;
  padding: 10px 20px;
  font-size: 1em;
  background-color: #409eff;
  color: white;
  border: none;
  border-radius: 5px;
  cursor: pointer;
  transition: all 0.3s ease;
}

.btn:hover {
  background-color: #66b1ff;
  transform: scale(1.05);
}

.active-btn {
  background-color: #ff5c8d;  /* 激活时背景色更鲜艳 */
  color: white;
  border: 2px solid #ff5c8d;  /* 激活时加上边框 */
  font-weight: bold;
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.15);  /* 激活时增加阴影 */
}

.active-btn:hover {
  background-color: #ff4a75; /* 激活按钮的悬停状态 */
  transform: scale(1.05);
}

.info-box {
  background: #f7f7f7;
  border: 1px solid #ddd;
  border-radius: 8px;
  padding: 20px;
  margin-bottom: 20px;
  box-shadow: 0 2px 5px rgba(0, 0, 0, 0.1);
}

.select-box {
  padding: 8px;
  margin-top: 10px;
  border-radius: 4px;
  border: 1px solid #ccc;
  width: 100%;
  max-width: 200px;
}

.parameters-container {
  background: #f7f7f7;
  border: 1px solid #ddd;
  border-radius: 8px;
  padding: 20px;
  margin-bottom: 20px;
  box-shadow: 0 2px 5px rgba(0, 0, 0, 0.1);
}

.parameters-container h3 {
  margin-bottom: 10px;
  color: #333;
}

.parameters-container p {
  margin: 8px 0;
  color: #555;
}

.details-box {
  margin-top: 10px;
  cursor: pointer;
}

.details-summary {
  font-weight: bold;
  color: #007bff;
  outline: none;
  font-size: 1em;
}

.details-summary:hover {
  text-decoration: underline;
}

.details-content {
  margin-top: 10px;
  color: #555;
}
</style>
