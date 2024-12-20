<template>
  <div class="manual-container">
    <section class="introduction">
      <h1>Introduction</h1>
      <p>This section provides a brief introduction to <strong>iSEE-tRNA</strong> (An Integrated Web Server for Design, Evaluating, and Visualizing Engineered sup-tRNA). Here, you will learn essential information about iSEE-tRNA, including its features, citation guidelines, and contact details for further inquiries.</p>

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
      <!-- <div class="faq-item">
        <p><strong>Q:</strong> What computational models and reference datasets are used to generate sup-tRNA sequences?</p>
        <p><strong>A:</strong> [Answer]</p>
      </div>
      <div class="faq-item">
        <p><strong>Q:</strong> What does the sup-tRNA sequence processing workflow look like?</p>
        <p><strong>A:</strong> [Answer]</p>
      </div>
      <div class="faq-item">
        <p><strong>Q:</strong> How does iSEE-tRNA present the sup-tRNA?</p>
        <p><strong>A:</strong> [Answer]</p>
      </div> -->
      <div class="faq-item">
        <p><strong>Q:</strong> How is Structure Folding Evaluation performed?</p>
        <p><strong>A:</strong> The structure folding parameters of each sequence, including the anticodon, infernal score, and tRNA position, are critical for understanding the structural features of the sup-tRNA.</p>
      </div>
      <div class="faq-item">
        <p><strong>Q:</strong> How is Identity Elements Evaluation performed?</p>
        <p><strong>A:</strong> The tREX Score algorithm is used to compare candidate sup-tRNAs with consensus templates derived from validated reference tRNA datasets, identifying conserved positions essential for amino acid binding and identity recognition.</p>
      </div>
      <div class="faq-item">
        <p><strong>Q:</strong> How is the binding affinity between aa-tRNAs and EF-Tu evaluated?</p>
        <p><strong>A:</strong> It is evaluated based on the free energy contributions of each sequence. The total free energy includes the amino acid ΔΔG° and the contributions from the tRNA T-stem base pairs. A total free energy value close to 0 indicates an optimal balance.</p>
      </div>
      <!-- <div class="faq-item">
        <p><strong>Q:</strong> How is the secondary structure predicted?</p>
        <p><strong>A:</strong> [Answer]</p>
      </div>
      <div class="faq-item">
        <p><strong>Q:</strong> How is the tertiary structure predicted?</p>
        <p><strong>A:</strong> [Answer]</p>
      </div> -->

      <h2>1.3 How to Cite iSEE-tRNA?</h2>
      <p>If you use iSEE-tRNA, please cite XX.</p>

      <h2>1.4 How to Contact Us?</h2>
      <p>For any inquiries or concerns regarding iSEE-tRNA, please contact us at <a href="mailto:shendekoudai@lovelumine.com">shendekoudai@lovelumine.com</a>.</p>
    </section>

    <section class="user-tutorial">
      <h1>User Tutorial</h1>
      <p>In this section, you will learn how to generate and evaluate the sup-tRNAs you need using iSEE-tRNA. To assist you, we have provided screenshots of various parts of the user interface, with key information highlighted in red boxes.</p>

      <h2>2.1 Sequence Generator</h2>
      <p>iSEE-tRNA generates specific sup-tRNA meeting user-defined requirements. You can select the stop codon in the Sequence Generator to generate the desired sup-tRNA. Below are the operation diagrams.</p>

      <h2>2.2 tRNA Evaluator</h2>
      <div class="evaluation-step">
        <h3>1) Structure Folding Evaluation</h3>
        <p>Displays the structure folding parameters of each sequence, including the anticodon, infernal score, and tRNA positions. Users can filter the results through the Infernal Score.</p>
      </div>
      <div class="evaluation-step">
        <h3>2) Identity Elements Evaluation</h3>
        <p>Provides tREX Scores for different amino acids. A positive tREX Score indicates that the generated tRNA likely possesses the correct identity elements to carry a specific amino acid and function as a sup-tRNA. Users can sort the results by tREX Score.</p>
      </div>
      <div class="evaluation-step">
        <h3>3) Binding Affinity Evaluation</h3>
        <p>Demonstrates the total free energy of different amino acids and sup-tRNAs.</p>
      </div>

      <h2>2.3 tRNA Visualization Analysis</h2>
      <p>In this part, you can view the predicted secondary and tertiary structures of tRNA sequences.</p>
    </section>
  </div>
</template>



<style scoped>
.manual-container {
  font-family: Arial, sans-serif;
  margin: 20px;
  line-height: 1.6;
}

h1 {
  color: #333;
  font-size: 2em;
  margin-bottom: 15px;
}

h2 {
  color: #333;
  font-size: 1.5em;
  margin-top: 20px;
}

h3 {
  color: #333;
  font-size: 1.2em;
  margin-top: 15px;
}

p {
  font-size: 1em;
  color: #555;
  margin-bottom: 15px;
}

.faq-item {
  margin-bottom: 10px;
}

.faq-item strong {
  color: #007bff;
}

a {
  color: #007bff;
}

a:hover {
  text-decoration: underline;
}

.user-tutorial {
  margin-top: 30px;
}

.evaluation-step {
  margin-top: 20px;
}

.manual-container p, .manual-container h2, .manual-container h3 {

  margin: 0 auto;
}
</style>
