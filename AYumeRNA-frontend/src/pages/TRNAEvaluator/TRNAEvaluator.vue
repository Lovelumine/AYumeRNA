<template>
  <div class="trna-evaluator">
    <h2>tRNA Evaluator</h2>
    <div class="buttons">
      <router-link to="/trna-evaluator/aminoacylation">
        <button class="btn">Aminoacylation Evaluation</button>
      </router-link>
      <router-link to="/trna-evaluator/structure-folding">
        <button class="btn">Structure Folding Evaluation</button>
      </router-link>
      <router-link to="/trna-evaluator/identity-elements">
        <button class="btn">Identity Elements Evaluation</button>
      </router-link>
    </div>

    <!-- 子路由渲染 -->
    <router-view></router-view>
  </div>
</template>

<script setup lang="ts">
</script>

<style scoped>
.trna-evaluator {
  text-align: center;
  margin: 20px;
}

.buttons {
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
}

.btn:hover {
  background-color: #66b1ff;
}
</style>
