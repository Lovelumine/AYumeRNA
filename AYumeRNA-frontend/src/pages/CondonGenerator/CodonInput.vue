<template>
  <div class="codon-input">
    <label for="amino-acid">Select Amino Acid:</label>
    <select id="amino-acid" v-model="aminoAcid">
      <option v-for="acid in aminoAcids" :key="acid" :value="acid">{{ acid }}</option>
    </select>

    <label for="species">Select Species:</label>
    <select id="species" v-model="species">
      <option v-for="sp in speciesList" :key="sp" :value="sp">{{ sp }}</option>
    </select>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue';

// Mock data
const aminoAcids = [
  'Alanine', 'Arginine', 'Asparagine', 'Aspartic acid', 'Cysteine',
  'Glutamine', 'Glutamic acid', 'Glycine', 'Histidine', 'Isoleucine',
  'Leucine', 'Lysine', 'Methionine', 'Phenylalanine', 'Proline',
  'Serine', 'Threonine', 'Tryptophan', 'Tyrosine', 'Valine'
];

const speciesList = [
  'Homo sapiens', 'Mus musculus', 'Escherichia coli', 'Saccharomyces cerevisiae',
  'Drosophila melanogaster', 'Caenorhabditis elegans'
];

const aminoAcid = ref(aminoAcids[0]);
const species = ref(speciesList[0]);
</script>

<style scoped>
.codon-input {
  display: flex;
  flex-direction: column;
  width: 100%;
  max-width: 400px;
  margin-bottom: 1.5em;
}

label {
  font-size: 1em;
  color: #2c3e50;
  margin-top: 0.5em;
}

select {
  padding: 0.8em;
  font-size: 1em;
  border: 1px solid #ddd;
  border-radius: 4px;
  margin-top: 0.5em;
  transition: border-color 0.3s ease;
  cursor: pointer;
}

select:focus {
  border-color: #4caf50;
  outline: none;
}
</style>
