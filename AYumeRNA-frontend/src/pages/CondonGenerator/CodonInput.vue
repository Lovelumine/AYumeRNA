<!-- src/pages/CondonGenerator/CodonInput.vue -->
<template>
  <div class="codon-input">
    <label for="amino-acid">Select Codon:</label>
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
  'TCA',
  'TTA',
  'CTA',
]

// const speciesList = ['All','Eukaryota', 'Bacteria', 'Archaea']
const speciesList = ['All']

// 定义状态变量
const aminoAcid = ref(aminoAcids[0])
const species = ref(speciesList[0])

// 计算属性，生成模型名称
const modelName = computed(() => {
  const model = `${aminoAcid.value}_${species.value}.pt`
  console.log('[computed] modelName updated to:', model)
  return model
})

// 在组件加载时读取本地存储的数据
onMounted(() => {
  console.log(
    '[onMounted] Component mounted. Attempting to load saved parameters.',
  )

  const storedParameters = localStorage.getItem('generationParameters')
  if (storedParameters) {
    console.log('[onMounted] Found stored parameters:', storedParameters)

    try {
      const params = JSON.parse(storedParameters)

      if (params.model) {
        console.log('[onMounted] Parsing model:', params.model)

        const [parsedAminoAcid, parsedSpecies] = params.model
          .replace('.pt', '')
          .split('_')

        console.log('[onMounted] Parsed aminoAcid:', parsedAminoAcid)
        console.log('[onMounted] Parsed species:', parsedSpecies)

        if (aminoAcids.includes(parsedAminoAcid)) {
          aminoAcid.value = parsedAminoAcid
          console.log('[onMounted] aminoAcid initialized to:', aminoAcid.value)
        } else {
          console.warn(
            '[onMounted] Parsed aminoAcid not in aminoAcids list:',
            parsedAminoAcid,
          )
        }

        if (speciesList.includes(parsedSpecies)) {
          species.value = parsedSpecies
          console.log('[onMounted] species initialized to:', species.value)
        } else {
          console.warn(
            '[onMounted] Parsed species not in speciesList:',
            parsedSpecies,
          )
        }
      }
    } catch (error) {
      console.error('[onMounted] Failed to parse stored parameters:', error)
    }
  } else {
    console.log('[onMounted] No stored parameters found in localStorage.')
  }

  // 触发一次 updateModel 事件，确保父组件接收到初始的 modelName
  emits('updateModel', modelName.value)
})

// 监听 aminoAcid 和 species 的变化，保存到 localStorage，并触发事件
watch([aminoAcid, species], () => {
  const parameters = {
    aminoAcid: aminoAcid.value,
    species: species.value,
    model: modelName.value,
  }

  console.log(
    '[watch] aminoAcid or species changed. Saving to localStorage:',
    parameters,
  )

  localStorage.setItem('generationParameters', JSON.stringify(parameters))

  console.log(
    '[watch] Emitting updateModel event with modelName:',
    modelName.value,
  )
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
