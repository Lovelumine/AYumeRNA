<template>
  <div class="codon-input">
    <label for="amino-acid">Select Amino Acid:</label>
    <select v-model="aminoAcid" id="amino-acid" class="dropdown">
      <option v-for="acid in aminoAcids" :key="acid" :value="acid">
        {{ acid }}
      </option>
    </select>

    <label for="species">Select Domain:</label>
    <select v-model="species" id="species" class="dropdown">
      <option v-for="sp in speciesList" :key="sp" :value="sp">
        {{ sp }}
      </option>
    </select>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted } from 'vue'
import { defineEmits } from 'vue'

const emits = defineEmits(['updateModel'])

// Mock data
const aminoAcids = [
  'Alanine',
  'Arginine',
  'Asparagine',
  'Aspartic acid',
  'Cysteine',
  'Glutamine',
  'Glutamic acid',
  'Glycine',
  'Histidine',
  'Isoleucine',
  'Leucine',
  'Lysine',
  'Methionine',
  'Phenylalanine',
  'Proline',
  'Serine',
  'Threonine',
  'Tryptophan',
  'Tyrosine',
  'Valine',
]

const speciesList = ['Eukaryota', 'Bacteria', 'Archaea']

// 定义状态变量
const aminoAcid = ref(aminoAcids[0])
const species = ref(speciesList[0])

// 在组件加载时读取本地存储的数据
onMounted(() => {
  const storedParameters = localStorage.getItem('generationParameters')
  if (storedParameters) {
    try {
      const params = JSON.parse(storedParameters)
      if (params.aminoAcid && params.species) {
        aminoAcid.value = params.aminoAcid
        species.value = params.species
        console.log('Loaded parameters from localStorage:', params)
      }
    } catch (error) {
      console.error('Failed to parse parameters from localStorage:', error)
    }
  }
})

// 计算属性，生成模型名称
const modelName = computed(() => {
  return `${aminoAcid.value}_${species.value}.pt`
})

// 监听 aminoAcid 和 species 的变化，保存到 localStorage，并触发事件
watch([aminoAcid, species], () => {
  const parameters = {
    aminoAcid: aminoAcid.value,
    species: species.value,
  }
  localStorage.setItem('generationParameters', JSON.stringify(parameters))
  console.log('Saved parameters to localStorage:', parameters)

  // 更新模型名称
  emits('updateModel', modelName.value)
})
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

.dropdown {
  font-family: 'Roboto', sans-serif;
  font-size: 1em;
  padding: 0.5em;
  border: 1px solid #ccc;
  border-radius: 5px;
  background-color: #fff;
  color: #333;
  cursor: pointer;
  width: 100%;
  max-width: 400px;
}

.model-selection {
  margin-top: 1.5em;
  display: flex;
  justify-content: center;
  align-items: center;
}

.model-text {
  font-size: 1.4em;
  font-family: 'Playfair Display', serif;
  color: #2c3e50;
  text-align: center;
  font-weight: bold;
  background-color: #f0f4f7;
  padding: 0.8em 1.5em;
  border-radius: 10px;
  box-shadow: 0 3px 8px rgba(0, 0, 0, 0.1);
  transition:
    transform 0.2s ease,
    box-shadow 0.2s ease;
}

.model-name {
  color: #007bff;
  font-weight: 600;
  margin-left: 8px;
}

.model-text:hover {
  transform: scale(1.03);
  box-shadow: 0 5px 10px rgba(0, 0, 0, 0.15);
}
</style>
