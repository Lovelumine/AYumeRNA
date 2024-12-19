<template>
  <div class="home">
    <!-- 介绍部分 -->
    <div class="intro">
      <h1>Welcome to <span class="highlight">iSEE-tRNA</span></h1>
      <p>The Ultimate Tool for Generating, Evaluating, and Visualizing Engineered sup-tRNA Sequences</p>
      <router-link to="/">
        <button class="start-btn">Start Generating</button>
      </router-link>
    </div>

    <!-- 描述部分 -->
    <div class="description">
      <h2><span class="highlight">iSEE-tRNA</span>: Innovating tRNA with Stop Codon Decoding Capability</h2>
      <p>
        <span class="highlight">iSEE-tRNA</span>, derived from "Integrated Stop codon Decoding for Engineered tRNA," is dedicated to enabling users to generate sup-tRNA sequences capable of decoding stop codons. This tool simplifies the process of designing tRNA sequences, even for those without prior knowledge of FASTA files or tRNA structures. Users can specify the desired amino acid and receive the corresponding tRNA sequence, tailored to decode stop codons such as UAA, UAG, and UGA.
      </p>
      <p>
        The project empowers scientists by automating the creation of <span class="highlight">sup-tRNA</span> sequences that decode stop codons. With features like amino acid customization, species selection, and codon adjustments, <span class="highlight">iSEE-tRNA</span> caters to various experimental setups. The backend implements advanced substitution and filtering methods, ensuring the generated sequences maintain biological functionality.
      </p>
      <div class="scg-container">
        <!-- 使用 Font Awesome 图标 -->
        <i class="fas fa-flask scg-icon"></i>
        <p class="scg-description">
          <span class="highlight">iSEE-tRNA</span> is designed to address the needs of the modern scientific community, allowing researchers to bypass complex manual sequence generation and focus on analyzing the sequences for critical research outcomes.
        </p>
      </div>

      <h3>Customizable Amino Acid Codon Generator</h3>
      <p>
        <span class="highlight">iSEE-tRNA</span> allows users to design custom codons, generating a series of tRNA sequences. These sequences are evaluated and scored based on their efficiency in decoding specific stop codons. The tool integrates sophisticated structure prediction techniques, such as AlphaFold for tertiary structure and R2DT for secondary structure, to enhance sequence validity.
      </p>
      <p>
        Furthermore, <span class="highlight">iSEE-tRNA</span> considers protein-binding interactions and aminoacyl-tRNA synthetase (ARS) activity, ensuring that the generated tRNA sequences can effectively participate in translation processes. This integration of structural and functional analysis makes <span class="highlight">iSEE-tRNA</span> a robust tool for cutting-edge molecular biology research.
      </p>

      <h3>Using Tools like tRNAscan-SE for Sequence Validation</h3>
      <p>
        To ensure the validity and functionality of the generated tRNA sequences, <span class="highlight">iSEE-tRNA</span> integrates tools like <a href="https://lowelab.ucsc.edu/tRNAscan-SE/" target="_blank">tRNAscan-SE</a>. This tool is designed to scan known tRNA sequences and identify functional tRNA genes by analyzing their structural features.
      </p>
      <p>
        After generating the tRNA sequences, the backend compares them with sequences derived from tRNAscan-SE to identify functional tRNAs in the natural world. By comparing these sequences with known functional tRNAs, <span class="highlight">iSEE-tRNA</span> can validate the generated sequences for their biological relevance and ensure that they meet the standards of real-world tRNA functionality.
      </p>
      <p>
        This comparison and validation process enables the identification of potential errors and guarantees that the generated tRNA sequences align with natural, functional tRNAs. It also helps to eliminate sequences that are likely non-functional or unsuitable for specific applications, increasing the reliability of the tool.
      </p>

      <h3>Key Features</h3>
      <ul class="features-list">
        <li>Automated generation of sup-tRNA sequences</li>
        <li>Customization options for amino acids, species, and codons</li>
        <li>Advanced secondary and tertiary structure prediction</li>
        <li>Integration with AlphaFold and R2DT for enhanced sequence analysis</li>
        <li>Real-time scoring of generated sequences for functional optimization</li>
        <li>Sequence validation through comparison with natural tRNAs using tRNAscan-SE</li>
      </ul>
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
  padding: 2em;
  min-height: 100vh;
  background-color: #f4f8fb; /* 柔和的背景色 */
  width: 100%;
  box-sizing: border-box;
  font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
}

/* 介绍部分 */
.intro {
  text-align: center;  /* 居中对齐 */
  margin-bottom: 3em;
  width: 100%;
}

.intro h1 {
  font-size: 3em;
  color: #2c3e50;
}

.intro p {
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

/* 描述部分 */
.description {
  text-align: left;  /* 左对齐 */
  width: 100%;
  margin: 0;  /* 移除自动居中 */
}

.description h2 {
  font-size: 2.5em;
  color: #2c3e50;
  margin-bottom: 1.5em;
}

.description p {
  font-size: 1.1em;
  color: #555;
  line-height: 1.8;
  margin-bottom: 1.5em;
}

.description h3 {
  font-size: 1.7em;
  color: #3a4a5a;
  margin-top: 1.5em;
  margin-bottom: 1.2em;
}

.features-list {
  list-style-type: none;
  padding-left: 0;
}

.features-list li {
  font-size: 1.1em;
  color: #555;
  margin-bottom: 1em;
  padding-left: 20px;
  position: relative;
}

.features-list li::before {
  content: "✔";
  color: #4caf50;
  position: absolute;
  left: 0;
  font-size: 1.3em;
  top: 0;
}

.scg-container {
  display: flex;
  align-items: center;
  margin: 2em 0;
}

.scg-icon {
  font-size: 3em;
  color: #4caf50;
  margin-right: 1em;
}

.scg-description {
  font-size: 1.1em;
  color: #555;
  max-width: 700px;
  line-height: 1.8;
}

/* 重点文字 */
.highlight {
  color: #e74c3c; /* 红色 */
  font-weight: bold;
}

/* 响应式设计：适配小屏幕 */
@media (max-width: 768px) {
  .home {
    padding: 1.5em;
  }

  .intro h1 {
    font-size: 2.5em;
  }

  .intro p {
    font-size: 1.1em;
  }

  .start-btn {
    padding: 0.8em 1.8em;
    font-size: 1em;
  }

  .description h2 {
    font-size: 2em;
  }

  .description p {
    font-size: 1em;
  }

  .description h3 {
    font-size: 1.5em;
  }

  .features-list li {
    font-size: 1em;
  }
}
</style>
