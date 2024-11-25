<template>
  <div class="tertiary-structure">
    <h2>Tertiary Structure Prediction</h2>
    <p class="description">
      We are currently working on integrating AlphaFold's capabilities into our platform. In the meantime, you can copy your tRNA sequence and use the <a href="https://alphafold.ebi.ac.uk/" target="_blank">AlphaFold Protein Structure Database</a> to generate the tertiary structure yourself.
    </p>

    <!-- Display Demo Image -->
    <div class="demo-image">
      <img src="@/pages/ResultAnalysis/TertiaryStructure/demo.png" alt="Tertiary Structure Demo" />
    </div>

    <!-- Optionally, you can display some sample structures or further instructions here -->
  </div>
</template>

<script setup lang="ts">
// No changes needed in the script section for this update
</script>

<style scoped>
.tertiary-structure {
  padding: 30px;
  margin: 0 auto;
  font-family: 'Arial', sans-serif;
  text-align: left;
  background-color: #f9f9f9;
  border-radius: 8px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
}

h2 {
  font-size: 2.5em;
  color: #2c3e50;
  font-weight: bold;
  margin-bottom: 20px;
}

.description {
  font-size: 1.2em;
  color: #555;
  margin-bottom: 30px;
  line-height: 1.6;
}

.description a {
  color: #409eff;
  text-decoration: none;
}

.description a:hover {
  text-decoration: underline;
}

.demo-image {
  text-align: center;
  margin-bottom: 30px;
}

.demo-image img {
  max-width: 100%;
  height: auto;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}
</style>
