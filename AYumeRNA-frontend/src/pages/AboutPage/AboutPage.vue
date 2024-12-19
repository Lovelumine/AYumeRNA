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
            <el-link href="mailto:shendekoudai@lovelumine.com">wuqh29@mail2.sysu.edu.cn</el-link>
            <br>College of Agriculture and Biotechnology, Sun Yat-sen University, Shenzhen, 518107, P. R. China
          </address>
        </div>
      </el-card>

      <!-- Tools Card -->
      <br>
      <el-card shadow="hover" class="box-card tools-card">
        <h2>Our Tools</h2>
        <p class="info-text">
          In addition to <span class="highlight">iSEE-tRNA</span>, we have developed several databases and software designed to investigate the biological functions and mechanisms of various RNAs in biological processes.
        </p>
        <div class="tools-grid">
          <tool-card
            v-for="(tool, index) in tools"
            :key="index"
            :link="tool.link"
            :imageSrc="tool.imageSrc"
            :imageAlt="tool.imageAlt"
            :title="tool.title"
            :description="tool.description"
          />
        </div>
      </el-card>
    </div>
  </div>
</template>

<script lang="ts">

export default {
  name: 'ContactPage',
  components: {

  },
  data() {
    return {
      tools: [
        {
          title: 'RMBase V2.0',
          link: 'http://rna.sysu.edu.cn/rmbase/',
          imageSrc: 'https://rna.sysu.edu.cn/rmbase3/pictures/contact/rmBaseLogo.png',
          imageAlt: 'RMBase Logo',
          description: 'Decoding the RNA modification map from epitranscriptome sequencing data. (Xuan et al. Nucleic Acids Res. 2018 Jan 4;46(D1):D327-34.)'
        },
        // ... other tools
      ]
    }
  }
}
</script>

<style scoped>
.site--main {
  padding: 20px;
}

.container-fluid {
  background-color: #e1f5fe;
  padding: 30px;
  text-align: justify;
  color: #0d47a1;
  border-radius: 10px;
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.1);
}

.box-card {
  margin-bottom: 20px;
  border-radius: 10px;
}

.contact-card,
.tools-card {
  background-color: #ffffff;
}

.tools-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(250px, 1fr));
  gap: 20px;
}

.tool-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  background-color: #e3f2fd;
  border-radius: 10px;
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.1);
  transition: transform 0.2s;
}

.tool-card:hover {
  transform: scale(1.05);
}

.tool-card h4 {
  margin: 10px 0;
  font-size: 16px;
}

.info-text {
  font-size: 14px;
}

.mail a {
  text-decoration: none;
  color: #0d47a1;
  font-weight: bold;
}

.tool-image {
  max-width: 100%;
  max-height: 150px;
  object-fit: contain;
}

@media (max-width: 1200px) {
  /* Additional responsive styles if needed */
}
</style>
