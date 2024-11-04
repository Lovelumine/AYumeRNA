<template>
  <div class="home">
    <div class="intro-card">
      <h1>Welcome to AYumeRNA</h1>
      <p>The Ultimate Tool for Generating Customized tRNA Sequences</p>
      <router-link to="/generator">
        <button class="start-btn">Start Generating</button>
      </router-link>
    </div>

    <div class="description-card">
      <h2>AYumeRNA: Aim Your Dream in RNA Innovation</h2>
      <p>
        AYumeRNA, derived from "Aim Your Dream," represents a commitment to enabling users to target their goals by generating RNA sequences with practical applications. This project supports the generation of specific amino acid tRNA sequences without the need for prior knowledge of FASTA files or TRA sequences.
        By simply specifying the desired amino acid, users can automatically generate the corresponding RNA sequence. Additionally, AYumeRNA offers options for selecting different species and codons to meet specific needs. During the training process, substitution or filtering methods ensure the generated sequences' validity and functionality.
      </p>
      <h3>Customizable Amino Acid Codon Generator</h3>
      <p>
        AYumeRNA allows users to design custom codons, while the backend generates a series of TMA sequences, scoring each to identify the optimal sequence. The project emphasizes the importance of secondary and tertiary structures and considers interactions with enzymes and aminoacyl-tRNA synthetases (ARS).
        This flexible approach accommodates species-specific variations. Additionally, AYumeRNA integrates AlphaFold to study protein-binding affinities and R2DT to predict secondary structures, enhancing functionality and expanding research possibilities.
      </p>
    </div>
  </div>
</template>


<script setup lang="ts">
// 页面逻辑可以在这里添加
</script>

<style scoped>
/* 整体布局 */
.home {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 2em;

  min-height: 100vh;
  background: linear-gradient(to right, #f0f4f8, #d9e2ec);
  width: 100%;
  min-height: 100%;
  box-sizing: border-box;
  font-family: Arial, sans-serif;
}

/* 介绍卡片 */
.intro-card {
  background-color: #fff;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  padding: 2em;
  text-align: center;
  margin-bottom: 2em;
  max-width: 600px;
  width: 100%;
}

.intro-card h1 {
  font-size: 2em;
  color: #2c3e50;
}

.intro-card p {
  font-size: 1.2em;
  color: #7b8b9a;
  margin: 1em 0;
}

.start-btn {
  background-color: #4caf50;
  color: white;
  border: none;
  padding: 0.8em 1.5em;
  font-size: 1em;
  font-weight: bold;
  border-radius: 5px;
  cursor: pointer;
  transition: background-color 0.3s ease;
}

.start-btn:hover {
  background-color: #45a049;
}

/* 描述卡片 */
.description-card {
  background-color: #fff;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  padding: 2em;
  text-align: left;
  max-width: 800px;
  width: 100%;
}

.description-card h2 {
  font-size: 1.8em;
  color: #2c3e50;
  margin-bottom: 1em;
}

.description-card p {
  font-size: 1em;
  color: #555;
  line-height: 1.6;
  margin-bottom: 1.5em;
}

.description-card h3 {
  font-size: 1.5em;
  color: #3a4a5a;
  margin-top: 1.5em;
  margin-bottom: 1em;
}
</style>
