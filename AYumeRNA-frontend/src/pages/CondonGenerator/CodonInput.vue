<template>
  <div class="codon-input">
    <label for="amino-acid">Select Amino Acid:</label>
    <div class="amino-acids">
      <label v-for="acid in aminoAcids" :key="acid" class="amino-acid-option">
        <input type="radio" :value="acid" v-model="aminoAcid" :id="acid" class="hidden-radio" />
        <span>{{ acid }}</span>
      </label>
    </div>

    <label for="species">Select Species:</label>
    <div class="species">
      <label v-for="sp in speciesList" :key="sp" class="species-option">
        <input type="radio" :value="sp" v-model="species" :id="sp" class="hidden-radio" />
        <span>{{ sp }}</span>
      </label>
    </div>
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
@import url('https://fonts.googleapis.com/css2?family=Roboto:wght@500&family=Playfair+Display:wght@700&display=swap');

.codon-input {
  display: flex;
  flex-direction: column;
  width: 100%;
  gap: 1.5em;
}

label {
  font-family: 'Roboto', sans-serif;
  font-weight: 600;
  font-size: 1.3em;
  color: #333;
}

.amino-acids,
.species {
  display: flex;
  flex-wrap: wrap;
  gap: 15px;
}

.amino-acid-option,
.species-option {
  font-size: 1.1em;
  color: #555;
  display: flex;
  align-items: center;
  cursor: pointer;
}

.amino-acid-option input[type="radio"],
.species-option input[type="radio"] {
  display: none;
}

.amino-acid-option span,
.species-option span {
  padding: 0.5em;
  border-radius: 5px;
  transition: background-color 0.3s ease;
}

.amino-acid-option:hover span,
.species-option:hover span {
  background-color: #4caf50;
  color: white;
}

.amino-acid-option input[type="radio"]:checked + span,
.species-option input[type="radio"]:checked + span {
  background-color: #4caf50;
  color: white;
}
</style>
