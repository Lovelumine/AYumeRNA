<template>
  <div class="codon-input">
    <label for="amino-acid">Select Amino Acid:</label>
    <div class="amino-acids">
      <label v-for="acid in aminoAcids" :key="acid" class="amino-acid-option">
        <input
          type="radio"
          :value="acid"
          v-model="aminoAcid"
          :id="acid"
          class="hidden-radio"
        />
        <span>{{ acid }}</span>
      </label>
    </div>

    <label for="species">Select Domains:</label>
    <div class="species">
      <label v-for="sp in speciesList" :key="sp" class="species-option">
        <input
          type="radio"
          :value="sp"
          v-model="species"
          :id="sp"
          class="hidden-radio"
        />
        <span>{{ sp }}</span>
      </label>
    </div>

    <div class="model-selection" v-if="modelName">
      <p class="model-text">
        Selected Model: <span class="model-name">{{ modelName }}</span>
      </p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted } from 'vue';
import { defineEmits } from 'vue';

const emits = defineEmits(['updateModel']);

// Mock data
const aminoAcids = [
  'Alanine', 'Arginine', 'Asparagine', 'Aspartic acid', 'Cysteine',
  'Glutamine', 'Glutamic acid', 'Glycine', 'Histidine', 'Isoleucine',
  'Leucine', 'Lysine', 'Methionine', 'Phenylalanine', 'Proline',
  'Serine', 'Threonine', 'Tryptophan', 'Tyrosine', 'Valine'
];

const speciesList = [
  'Eukaryota', 'Bacteria', 'Archaea',
];

// 定义状态变量
const aminoAcid = ref(aminoAcids[0]);
const species = ref(speciesList[0]);

// 在组件加载时读取本地存储的数据
onMounted(() => {
  const storedParameters = localStorage.getItem('generationParameters');
  if (storedParameters) {
    try {
      const params = JSON.parse(storedParameters);
      if (params.aminoAcid && params.species) {
        aminoAcid.value = params.aminoAcid;
        species.value = params.species;
        console.log('Loaded parameters from localStorage:', params);
      }
    } catch (error) {
      console.error('Failed to parse parameters from localStorage:', error);
    }
  }
});

// 计算属性，生成模型名称
const modelName = computed(() => {
  return `${aminoAcid.value}_${species.value}.pt`;
});

// 监听 aminoAcid 和 species 的变化，保存到 localStorage，并触发事件
watch([aminoAcid, species], () => {
  const parameters = {
    aminoAcid: aminoAcid.value,
    species: species.value,
  };
  localStorage.setItem('generationParameters', JSON.stringify(parameters));
  console.log('Saved parameters to localStorage:', parameters);

  // 更新模型名称
  emits('updateModel', modelName.value);
});
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
  font-size: 1.2em;
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
  font-size: 1em;
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
  padding: 0.4em;
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

.model-selection {
  margin-top: 1.5em;
  display: flex;
  justify-content: center;
  align-items: center;
}

.model-text {
  font-size: 1.4em; /* 减小整体字体大小 */
  font-family: 'Playfair Display', serif;
  color: #2c3e50;
  text-align: center;
  font-weight: bold;
  background-color: #f0f4f7;
  padding: 0.8em 1.5em; /* 减小内边距 */
  border-radius: 10px; /* 减小圆角 */
  box-shadow: 0 3px 8px rgba(0, 0, 0, 0.1); /* 调整阴影 */
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.model-name {
  color: #007BFF;
  font-weight: 600;
  margin-left: 8px; /* 减小间距 */
}

.model-text:hover {
  transform: scale(1.03); /* 减小放大比例 */
  box-shadow: 0 5px 10px rgba(0, 0, 0, 0.15);
}

.label {
  color: #333;
  font-size: 1em; /* 调整 Selected Model 的大小 */
}
</style>
