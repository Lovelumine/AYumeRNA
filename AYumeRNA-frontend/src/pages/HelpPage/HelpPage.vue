<template>
  <div class="tutorial-container">
    <h1>1. Introduction</h1>
    <p>
      This section provides a brief introduction to iSEE-tRNA (An Integrated Web Server for Design, Evaluating, and Visualizing Engineered sup-tRNA). Here, you will learn essential information about iSEE-tRNA, including its features, citation guidelines, and contact details for further inquiries.
    </p>
    <h2>1.1 About iSEE-tRNA</h2>
    <p>
      iSEE-tRNA is an integrated web server designed to streamline the processes of sequence generation, tRNA evaluation, and visualization, providing users with sup-tRNA (suppressor tRNA) capable of decoding stop codons. It combines advanced AI-driven tools and analytical methods to provide an efficient and user-friendly platform for researchers.
    </p>
    <p>
      Firstly, the Sequence Generator produces a batch of candidate sup-tRNAs that meet user-defined requirements using artificial intelligence, leveraging deep generative models and reference datasets.
    </p>
    <p>
      Secondly, the tRNA Evaluator assesses these candidate sup-tRNAs in three key aspects to confirm their ability to carry specific amino acids and decode the stop codons. The evaluation includes structure folding analysis, identity element evaluation, and analysis of the binding affinity between aa-tRNAs and EF-Tu.
    </p>
    <p>
      Thirdly, the Visual Analysis module presents the predicted secondary and tertiary structures of the tRNA sequences.
    </p>
    <p>
      iSEE-tRNA is a powerful web tool for molecular biology research by simplifying the design of tRNA sequences. It enables researchers to bypass the complexities of manual sequence generation and focus on analyzing the sequences to achieve key research results.
    </p>

    <h2>1.2 Methods for Generation, Evaluation, and Prediction of iSEE-tRNA Sequences</h2>
    <p><strong>Q:</strong> What computational models and reference datasets are used to generate sup-tRNA sequences?</p>
    <p><strong>A:</strong> GtRNAdb dataset, using Rfamgen for random sequence generation based on covariance models and hidden Markov models, machine learning methods for sequence generation. tRNAscan-se for secondary structure prediction and classification. tREx score to determine which amino acid tRNA class the sequence is more likely to belong to. Alphafold for tertiary structure prediction.</p>

    <p><strong>Q:</strong> What does the sup-tRNA sequence processing workflow look like?</p>
    <p><strong>A:</strong></p>
    <ul>
      <li><strong>Dataset Selection:</strong> Using the GtRNAdb dataset, the web server filters the dataset based on selected codons.</li>
      <li><strong>Sequence Generation:</strong> Using Rfamgen, random sequences are generated based on covariance models and hidden Markov models through machine learning methods.</li>
      <li><strong>Structure Analysis:</strong>
        <ul>
          <li>Using R2DT for secondary structure prediction and visualization.</li>
          <li>Using AlphaFold for tertiary structure prediction and visualization, considering spatial conformation effects.</li>
        </ul>
      </li>
      <li><strong>Function Evaluation:</strong> Using tREx scoring with reference to classical endogenous tRNAs for each amino acid, determining sequence preferences for certain tRNA types and identity element confirmation.</li>
      <li><strong>Energy Calculation and Screening:</strong> Energy calculations with specific aminoacyl-tRNA synthetases to further screen target sup-tRNAs.</li>
    </ul>

    <h2>2. User Tutorial</h2>
    <p>
      In this section, you will learn how to generate and evaluate the sup-tRNAs you need using iSEE-tRNA. To assist you, we have provided screenshots of various parts of the user interface, with key information highlighted in red boxes.
    </p>

    <h2>2.1 Sequence Generator</h2>
    <p>
      iSEE-tRNA generates specific sup-tRNA meeting user-defined requirements. You can select the stop codon in the Sequence Generator to generate the desired sup-tRNA. Below are the operation diagrams.
    </p>
    <div class="image-container">
      <img
        src="https://minio.lumoxuan.cn/ayumerna/sample/help1.jpg"
        alt="User tutorial workflow"
      />
    </div>
    <h2>2.2 tRNA Evaluator</h2>
    <p>
      (1) Structure Folding Evaluation displays the structure folding parameters of each sequence, including the anticodon, infernal score, and tRNA positions. Users can filter the results through the Infernal Score.
    </p>
    <p>
      (2) Identity Elements Evaluation provides tREX Scores for different amino acids. A positive tREX Score indicates that the generated tRNA likely possesses the correct identity elements to carry a specific amino acid and function as a sup-tRNA. Users can sort the results by tREX Score.
    </p>
    <div class="image-container">
      <img
        src="https://minio.lumoxuan.cn/ayumerna/sample/help2.jpg"
        alt="User tutorial workflow"
      />
    </div>
    <p>
      (3) The binding affinity between aa-tRNAs and EF-Tu demonstrates the total free energy of different amino acids and sup-tRNAs.
    </p>
    <div class="image-container">
      <img
        src="https://minio.lumoxuan.cn/ayumerna/sample/help3.jpg"
        alt="User tutorial workflow"
      />
    </div>
    <h2>2.3 tRNA Visualization Analysis</h2>
    <p>
      In this part, you can view the predicted secondary and tertiary structures of tRNA sequences.
    </p>

  </div>
</template>

<script setup lang="ts"></script>

<style scoped>
.tutorial-container {
  padding: 20px;
  font-family: Arial, sans-serif;
}

h1,
h2 {
  color: #333;
}

p,
ul {
  font-size: 1.2em;
  color: #555;
}

ul {
  list-style-type: disc;
  margin-left: 20px;
}

li {
  font-size: 0.9em; /* Ensures consistent font size for list items */
  margin-bottom: 10px;
}

strong {
  font-weight: bold;
}

/* Style for the image container */
.image-container {
  width: 100%;
  max-height: 500px; /* Limit the height */
  overflow-y: auto; /* Add vertical scroll bar if the content overflows */
  overflow-x: hidden;
  border: 1px solid #ddd;
  border-radius: 10px;
}

.image-container img {
  width: 100%; /* Full-width image */
  height: auto; /* Maintain aspect ratio */
  display: block;
}
</style>
