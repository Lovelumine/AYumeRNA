<!-- src/pages/AboutPage/AboutPage.vue -->
<template>
  <div class="site--main">
    <div class="container-fluid">
      <!-- Contact Us Card -->
      <el-card shadow="hover" class="box-card contact-card">
        <h2>Contact Us</h2>
        <p class="info-text">
          <span class="highlight">iSEE-tRNA</span> is continuously maintained and improved. We appreciate any questions, feedback, or suggestions regarding its usage.
        </p>
        <div class="contact-info">
          <address class="mail">
            <strong>Lingling Zheng:</strong>
            <el-link href="mailto:zhengll33@mail.sysu.edu.cn">zhengll33@mail.sysu.edu.cn</el-link>
            <br>RNA Information Center, State Key Laboratory for Biocontrol, Sun Yat-sen University, Guangzhou 510275, P. R. China
          </address>
          <address class="mail">
            <strong>Zhuo Ouyang:</strong>
            <el-link href="mailto:shendekoudai@lovelumine.com">shendekoudai@lovelumine.com</el-link>
            <br>RNA Information Center, State Key Laboratory for Biocontrol, Sun Yat-sen University, Guangzhou 510275, P. R. China
          </address>
          <address class="mail">
            <strong>Qiuhui Wu:</strong>
            <el-link href="mailto:wuqh29@mail2.sysu.edu.cn">wuqh29@mail2.sysu.edu.cn</el-link>
            <br>College of Agriculture and Biotechnology, Sun Yat-sen University, Shenzhen, 518107, P. R. China
          </address>
        </div>
      </el-card>

      <!-- Tools Card -->
      <el-card shadow="hover" class="box-card tools-card">
        <h2>Our Tools</h2>
        <p class="info-text">
          In addition to <span class="highlight">iSEE-tRNA</span>, we have developed several databases and software designed to investigate the biological functions and mechanisms of various RNAs in biological processes.
        </p>
        <div class="tools-grid">
          <ToolCard
            v-for="(tool, index) in tools"
            :key="index"
            :link="tool.link"
            :imageSrc="tool.imageSrc"
            :imageAlt="tool.imageAlt"
            :title="tool.title"
            :description="tool.description"
          />
          <!-- 可以在此添加更多工具 -->
        </div>
      </el-card>
    </div>
  </div>
</template>

<script lang="ts">
import ToolCard from './ToolCard.vue' // 导入 ToolCard 组件

export default {
  name: 'AboutPage',
  components: {
    ToolCard
  },
  data() {
    return {
      tools: [
  {
    title: 'RMBase V2.0',
    link: 'http://rna.sysu.edu.cn/rmbase/',
    imageSrc: 'https://rna.sysu.edu.cn/rmbase3/pictures/contact/rmBaseLogo.png',
    imageAlt: 'RMBase Logo',
    description: 'Deciphering the Map of RNA Modifications from Epitranscriptome Sequencing Data. (Xuan et al. Nucleic Acids Res. 2018 Jan 4;46(D1):D327-34.)'
  },
  {
    title: 'ENCORI',
    link: 'http://starbase.sysu.edu.cn/index.php',
    imageSrc: 'https://rna.sysu.edu.cn/rmbase3/pictures/contact/starBase_logo.png',
    imageAlt: 'StarBase Logo',
    description: 'A database for exploring miRNA-mRNA and miRNA-ncRNA interaction maps from Argonaute and other RBP CLIP-Seq data. (Li et al. Nucleic Acids Res. 2014;42:D92-7.)'
  },
  {
    title: 'ChIPBase v2.0',
    link: 'http://rna.sysu.edu.cn/chipbase',
    imageSrc: 'https://rna.sysu.edu.cn/rmbase3/pictures/contact/contact-chipbase-logo.png',
    imageAlt: 'ChIPBase Logo',
    description: 'Decoding transcriptional regulatory networks of non-coding RNAs and protein-coding genes from ChIP-seq data. (Zhou et al. Nucleic Acids Res. 2017 Jan 04;45(D1):D43-D50.)'
  },

  {
    title: 'deepBase v3.0',
    link: 'http://deepbase.sysu.edu.cn/SnoSeekerNGS.php',
    imageSrc: 'https://rna.sysu.edu.cn/rmbase3/pictures/contact/contact-snoseeker-logo.png',
    imageAlt: 'snoSeeker Logo',
    description: 'deepBase v3.0: expression atlas and interactive analysis of ncRNAs from thousands of deep-sequencing data. (Xie et al. Nucleic Acids Res. 2021 Jan 8;49(D1):D877-D883.)'
  },
  {
    title: 'tRF2Cancer',
    link: 'http://rna.sysu.edu.cn/tRFfinder/',
    imageSrc: 'https://rna.sysu.edu.cn/rmbase3/pictures/contact/contact-tRF2Cancer-logo.png',
    imageAlt: 'tRF2Cancer Logo',
    description: 'A web server to detect tRNA-derived small RNA fragments (tRFs) and their expression in multiple cancers. (Zheng et al. Nucleic Acids Res. 2016 Jul 8;44(W1):W185-93.)'
  },
  {
    title: 'dreamBase',
    link: 'http://rna.sysu.edu.cn/dreamBase/',
    imageSrc: 'https://rna.sysu.edu.cn/rmbase3/pictures/contact/contact-dreambase-logo.png',
    imageAlt: 'StarScan Logo',
    description: 'DNA modification, RNA regulation and protein binding of expressed pseudogenes in human health and disease. (Zheng et al. Nucleic Acids Res. 2018 Jan 4;46(D1):D85-D91.)'
  },  {
    title: 'StarScan',
    link: 'http://mirlab.sysu.edu.cn/starscan/index.php',
    imageSrc: 'https://rna.sysu.edu.cn/rmbase3/pictures/contact/contact-starscan-logo.png',
    imageAlt: 'dreamBase Logo',
    description: 'A web server for scanning small RNA targets from degradome sequencing data. (Liu et al. Nucleic Acids Res. 2015 May 18.)'
  },
  {
    title: 'Pol3Base',
    link: 'https://rna.sysu.edu.cn/pol3base/index.php',
    imageSrc: 'https://rna.sysu.edu.cn/rmbase3/pictures/contact/pol3base_logo.svg',
    imageAlt: 'Pol3Base Logo',
    description: 'Pol3Base: a resource for decoding the interactome, expression, evolution, epitranscriptome and disease variations of Pol III-transcribed ncRNAs. (Li Cai, Jiajia Xuan, et al. Nucleic Acids Res. 2021 Oct 15.)'
  },  {
    title: 'tModBase',
    link: 'https://www.tmodbase.com/#/',
    imageSrc: 'https://minio.lumoxuan.cn/ayumerna/sample/WX20241220-150139@2x.png',
    imageAlt: 'tModBase Logo',
    description: 'tModBase is a comprehensive resource that maps 27 types of tRNA modifications and their enzymes across human and mouse tRNAs, integrating multi-omics data to explore their dynamic changes, tissue-specific patterns, and links to diseases and cancers.'
  }
]
    }
  }
}
</script>

<style scoped>
.site--main {
  padding: 40px 20px;
  background-color: #f5f5f5;
  min-height: 100vh;
}

.container-fluid {
  max-width: 1200px;
  margin: 0 auto;
}

.box-card {
  margin-bottom: 30px;
  border-radius: 10px;
}

.contact-card,
.tools-card {
  background-color: #ffffff;
  padding: 30px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.contact-card h2,
.tools-card h2 {
  color: #0d47a1;
  margin-bottom: 20px;
}

.info-text {
  font-size: 16px;
  color: #555;
  line-height: 1.6;
  margin-bottom: 25px;
}

.highlight {
  color: #d32f2f; /* 红色突出显示 */
  font-weight: bold;
}

.contact-info {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.mail {
  font-style: normal;
}

.mail strong {
  color: #0d47a1;
}

.mail a {
  text-decoration: none;
  color: #1976d2;
  font-weight: bold;
}

.mail a:hover {
  text-decoration: underline;
}

.tools-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 20px;
}
</style>
