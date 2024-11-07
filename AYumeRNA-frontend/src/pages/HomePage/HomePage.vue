<template>
  <div class="home">
    <div class="intro-card">
      <h1>Welcome to AYumeRNA</h1>
      <p>The Ultimate Tool for Generating Stop Codon-Decoding Sup-tRNA Sequences</p>
      <router-link to="/generator">
        <button class="start-btn">Start Generating</button>
      </router-link>
    </div>

    <div class="description-card">
      <h2>AYumeRNA: Innovating tRNA with Stop Codon Decoding Capability</h2>
      <p>
        AYumeRNA, derived from "Aim Your Dream," is dedicated to enabling users to generate sup-tRNA sequences capable of decoding stop codons. This project allows users to generate tRNA sequences without prior knowledge of FASTA files or tRNA sequences. By simply specifying the desired amino acid, users can automatically generate the corresponding tRNA sequence, with a focus on stop codon decoding.
        AYumeRNA supports the selection of different species and codons to meet specific experimental needs. During the sequence generation process, substitution or filtering methods are applied to ensure the validity and functionality of the sequences, particularly when it comes to decoding stop codons like UAA, UAG, and UGA.
      </p>
      <h3>Customizable Amino Acid Codon Generator</h3>
      <p>
        AYumeRNA allows users to design custom codons, and the backend generates a series of tRNA sequences, scoring each to identify the optimal one. The project places special emphasis on secondary and tertiary structures and considers interactions with enzymes and aminoacyl-tRNA synthetases (ARS).
        Furthermore, AYumeRNA integrates AlphaFold to study protein-binding affinities and R2DT for secondary structure prediction, enhancing the functionality of the generated sequences and expanding the research potential, especially in the context of stop codon skipping and misreading in translation.
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
  background-color: #f4f8fb; /* 柔和的背景色 */
  width: 100%;
  box-sizing: border-box;
  font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
  overflow-y: auto;
}

/* 介绍卡片 */
.intro-card {
  background-color: #ffffff;
  border-radius: 10px;
  box-shadow: 0 6px 20px rgba(0, 0, 0, 0.1); /* 更柔和的阴影效果 */
  padding: 3em;
  text-align: center;
  margin-bottom: 3em;
  max-width: 600px;
  width: 100%;
  transition: transform 0.3s ease;
}

.intro-card:hover {
  transform: translateY(-5px); /* 鼠标悬停效果 */
}

.intro-card h1 {
  font-size: 2.5em;
  color: #2c3e50;
}

.intro-card p {
  font-size: 1.3em;
  color: #7b8b9a;
  margin: 1.5em 0;
}

.start-btn {
  background-color: #4caf50;
  color: white;
  border: none;
  padding: 0.9em 2em;
  font-size: 1.1em;
  font-weight: bold;
  border-radius: 8px;
  cursor: pointer;
  transition: background-color 0.3s ease, transform 0.2s ease;
}

.start-btn:hover {
  background-color: #45a049;
  transform: scale(1.05); /* 按钮悬停放大效果 */
}

/* 描述卡片 */
.description-card {
  background-color: #ffffff;
  border-radius: 10px;
  box-shadow: 0 6px 20px rgba(0, 0, 0, 0.1);
  padding: 3em;
  text-align: left;
  max-width: 800px;
  width: 100%;
}

.description-card h2 {
  font-size: 2em;
  color: #2c3e50;
  margin-bottom: 1.5em;
}

.description-card p {
  font-size: 1.1em;
  color: #555;
  line-height: 1.8;
  margin-bottom: 1.5em;
}

.description-card h3 {
  font-size: 1.7em;
  color: #3a4a5a;
  margin-top: 1.5em;
  margin-bottom: 1.2em;
}

/* 响应式设计：适配小屏幕 */
@media (max-width: 768px) {
  .home {
    padding: 1.5em;
  }

  .intro-card, .description-card {
    padding: 2em;
  }

  .intro-card h1 {
    font-size: 2em;
  }

  .intro-card p {
    font-size: 1.1em;
  }

  .start-btn {
    padding: 0.8em 1.8em;
    font-size: 1em;
  }
}
</style>
