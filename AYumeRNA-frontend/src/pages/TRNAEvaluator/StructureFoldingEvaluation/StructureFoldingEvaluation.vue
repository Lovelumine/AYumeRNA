<!-- src/pages/TRNAEvaluator/StructureFoldingEvaluation/StructureFoldingEvaluation.vue -->
<template>
  <div class="structure-folding-evaluation">
    <h3 class="title">Structure Folding Evaluation</h3>
    <p class="description">
      The table below displays the structure folding parameters of each sequence, including anticodon, infernal score, and tRNA positions.
      These parameters are key to understanding the structural features of the tRNA.
    </p>

    <!-- 用户输入序列的表单 -->
    <el-card shadow="hover" class="input-card">
      <h4>Input New Sequences</h4>
      <p class="input-description">
        You can enter your own sequences for processing. You are not limited to using system-generated sequences. Please enter one sequence per line (only A, U, C, G allowed).
      </p>

      <!-- 自定义折叠框，仅折叠输入区域 -->
      <div class="collapse-container">
        <button class="collapse-button" @click="toggleCollapse">
          {{ isCollapsed ? 'Expand Input' : 'Collapse Input' }}
        </button>
        <div v-show="!isCollapsed" class="form-container">
          <div class="button-group">
            <el-button type="primary" size="small" @click="loadExample">Load Example</el-button>
          </div>
          <el-form :model="sequenceInput" :rules="sequenceRules" ref="formRef" label-width="120px" @submit.prevent="handleSubmit">
            <el-form-item label="Enter Sequences" prop="sequences">
              <textarea
                v-model="sequenceInput.sequences"
                placeholder="Enter one sequence per line (only A, U, C, G allowed)"
                maxlength="500"
                class="custom-textarea"
                rows="6"
              ></textarea>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="handleSubmit">Submit Sequences</el-button>
              <el-button @click="resetInput">Reset</el-button>
            </el-form-item>
          </el-form>
        </div>
      </div>
    </el-card>

    <!-- 多选操作按钮 -->
    <div class="action-buttons">
      <ElButton type="primary" @click="downloadSelectedSequences" :disabled="selectedRows.length === 0">
        Download Selected Sequences
      </ElButton>
      <ElButton type="success" @click="handleNavigate">
        Navigate to Identity Elements
      </ElButton>
    </div>

    <!-- 使用自定义样式的表格展示 -->
    <s-table-provider :locale="en">
      <s-table
        :columns="columns"
        :data-source="dataSource"
        :max-height="400"
        row-key="key"
        :row-selection="rowSelection"
        :highlight-selected="highlightSelected"
        class="styled-table"
      >
        <template #bodyCell="{ text, column, record }">
          <template v-if="column.key === 'sequence'">
            <a>{{ text }}</a>
          </template>
          <template v-else-if="column.key === 'anticodon'">
            {{ record.anticodon }}
          </template>
          <template v-else-if="column.key === 'infernalScore'">
            <span v-if="typeof record.infernalScore === 'number'">
              {{ record.infernalScore }}
            </span>
            <span v-else>
              {{ record.infernalScore }}
            </span>
          </template>
          <template v-else-if="column.key === 'tRNAStart'">
            {{ record.tRNAStart }}
          </template>
          <template v-else-if="column.key === 'tRNAEnd'">
            {{ record.tRNAEnd }}
          </template>
          <template v-else-if="column.key === 'tRNAType'">
            {{ record.tRNAType }}
          </template>
          <template v-else-if="column.key === 'actions'">
            <button class="action-btn" @click="handleAnalyzeSequence(record)">
              Visualization
            </button>
          </template>
        </template>
      </s-table>
    </s-table-provider>

    <!-- 自定义弹出提示框 -->
    <div v-if="showCustomModal" class="modal-overlay" @click.self="closeModal">
      <div class="modal-content">
        <h3>Warning</h3>
        <p>Please select at least one sequence first.</p>
        <button class="modal-button" @click="closeModal">OK</button>
      </div>
    </div>

    <!-- 加载和错误提示 -->
    <div v-if="loading" class="loading">Loading...</div>
    <div v-if="error" class="error">{{ error }}</div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ElButton, ElForm, ElFormItem, ElCard, ElMessage } from 'element-plus'
import en from '@shene/table/dist/locale/en'
import { columns, defaultRowSelection } from './tableConfig'
import type { Key, SequenceInfo, Sequence } from './tableConfig'
import {
  dataSource,
  loading,
  error,
  loadSequences,
  handleAnalyzeSequence,
  updateSequences
} from './logic'
import { useRouter } from 'vue-router'
import type { STableRowSelection } from '@shene/table'
import type { FormInstance, FormRules } from 'element-plus'

// 初始化路由
const router = useRouter()

// 控制高亮选择项
const highlightSelected = ref(true)

// 存储选中的行
const selectedRowKeys = ref<Key[]>([])
const selectedRows = ref<SequenceInfo[]>([])

// 定义 rowSelection
const rowSelection = computed<STableRowSelection<SequenceInfo>>(() => ({
  ...defaultRowSelection,
  onChange: (keys: Key[], rows: SequenceInfo[]) => {
    selectedRowKeys.value = keys
    selectedRows.value = rows
  }
}))

// 用户输入的序列
const sequenceInput = ref({
  sequences: '' // Initially empty
})

// 表单引用
const formRef = ref<FormInstance>()

// 表单验证规则
const sequenceRules: FormRules = {
  sequences: [
    {
      validator: (rule, value: string, callback: (error?: Error) => void) => {
        if (!value.trim()) {
          callback(new Error('Please enter at least one sequence.'))
        } else {
          const sequences = value.split('\n').map(seq => seq.trim()).filter(seq => seq.length > 0)
          const invalidSequences = sequences.filter(seq => !/^[AUCGaucg]+$/.test(seq))
          if (invalidSequences.length > 0) {
            callback(new Error(`The following sequences are incorrect (only A, U, C, G allowed):\n${invalidSequences.join('\n')}`))
          } else {
            callback()
          }
        }
      },
      trigger: 'blur'
    }
  ]
}

// 自定义折叠状态
const isCollapsed = ref(true)

// 切换折叠状态
const toggleCollapse = () => {
  isCollapsed.value = !isCollapsed.value
}

// Load multiple example sequences
const loadExample = () => {
  sequenceInput.value.sequences = `
    GUGUCUGUAGCUUAGUUGGUAAAAGUGCAGCACUCUAAAUGCUGAGAAUGUGGGUUCGACUCCCACCAGACACA
    GGGUGUGUAGCCUAGUGGUAAAGGCAUCAGACUGUAAAUCUGAAGAAUGUGGGUUCGACUCCCACCACACCCA
    GUCUCUAUAGCUUAGUUUGGUAAAAGCAUCAGACAUGUUUAAAAAUUCUAAUUUCAUACGUUCGACUCGCACUAGAGACA
    GUCUCUGUGGCUUAGUUGGUAAAAGCGUACCACUCUAAAUGGUAAGGACGUGCGUUCGAAUCGCACCAGAGACA
    GGGGCUGUGGCCUAGCUGGUAAAGGCACUGGACUGUAAAUCCAGAGAGUGUGAGUUCGACUCUCACCAGCCCCA
    GUGUCUGUAGCUUAGUGGUAAAAGCAGCAGACUGUAAAAUUCUGAGGAUAUCCGUUCGAAUCGUACCAGAGACA
    GGAUCUGUAGCCUAGUGGUGAAAAGGAUCAGACUGUAAAUCUGAGGAUGUGGGUUCGACUCCCACCUUGACACA
    GUCUCUAUAGCUUAGUUGGUAAAAGGCAGCACUCUAAAUGCUGAGGACGUGGGUUCGACUCCCACCAGAGACA
    GUCUCUGUAGCUUAGUGGUAAAAGGGAGCACACUGUAAAAUGCUGAGGAUGUGGGUUCAAAUCCCACCAGACACA
    GGGUCUGUAGCUUAGUUGGUAAAGGCAGCACUCUAAAUGCUGAGGAUGUGGGUUCGACUCCCACCAGACACA
  `;
}

// 处理表单提交
const handleSubmit = () => {
  if (formRef.value) {
    formRef.value.validate((valid: boolean) => {
      if (valid) {
        const inputText = sequenceInput.value.sequences.trim()
        // 将输入的文本按行分割，并去除空行
        const newSequencesArray = inputText
          .split('\n')
          .map(seq => seq.trim())
          .filter(seq => seq.length > 0)

        // 构建 Sequence 数组，分配唯一的 key
        const timestamp = new Date().getTime().toString()
        const newSequences: Sequence[] = newSequencesArray.map((seq, index) => ({
          key: `${timestamp}_${index}`,
          sequence: seq.toUpperCase() // 转换为大写
        }))

        // 更新序列和时间戳
        updateSequences(newSequences)

        // 清空输入框
        sequenceInput.value.sequences = ''

        // 提供成功反馈
        ElMessage.success('The new sequences have been successfully uploaded and overwrite the previous ones.')
      }
    })
  }
}

// 重置输入框
const resetInput = () => {
  sequenceInput.value.sequences = ''
  ElMessage.info('Input has been reset.')
}

// 定义下载选中序列的函数
const downloadSelectedSequences = () => {
  if (selectedRows.value.length === 0) return

  // 将选中的序列转换为 CSV 格式
  const headers = ['key', 'sequence', 'anticodon', 'infernalScore', 'tRNAStart', 'tRNAEnd', 'tRNAType']
  const csvContent = [
    headers.join(','), // Header row
    ...selectedRows.value.map(row =>
      `${row.key},"${row.sequence}","${row.anticodon}","${row.infernalScore}","${row.tRNAStart}","${row.tRNAEnd}","${row.tRNAType}"`
    )
  ].join('\n')

  // 创建一个 Blob 对象
  const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' })
  const url = URL.createObjectURL(blob)

  // 创建一个临时链接并触发下载
  const link = document.createElement('a')
  link.setAttribute('href', url)
  link.setAttribute('download', 'selected_sequences.csv')
  link.style.visibility = 'hidden'
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
}

// 定义处理导航到 Identity Elements 的函数
const handleNavigate = () => {
  if (selectedRows.value.length === 0) {
    // 使用自定义的弹出提示框
    showCustomModal.value = true
    return
  }

  // 将选中的序列转换为缓存格式
  const cachedSequencesAfterStepOne = selectedRows.value.map(row => ({
    key: row.key,
    sequence: row.sequence,
    anticodon: row.anticodon,
    infernalScore: row.infernalScore,
    tRNAStart: row.tRNAStart,
    tRNAEnd: row.tRNAEnd,
    tRNAType: row.tRNAType,
  }))

  // 获取当前时间戳
  const timestamp = new Date().toISOString()

  // 存储时间戳到 localStorage
  localStorage.setItem('timestamp_cached_sequences_after_stepone', timestamp)

  // 存储选中的序列到 localStorage
  localStorage.setItem('cached_sequences_after_stepone', JSON.stringify(cachedSequencesAfterStepOne))

  // 跳转到 identity-elements 页面
  router.push('/trna-evaluator/identity-elements').then(() => {
    ElMessage.success('Navigated to Identity Elements page.')
    console.log('Navigated to /trna-evaluator/identity-elements')
  })
}

// 自定义弹出提示框控制
const showCustomModal = ref(false)

// 关闭弹出提示框
const closeModal = () => {
  showCustomModal.value = false
}

// 加载数据在组件挂载时
onMounted(() => {
  loadSequences()
})
</script>

<style scoped>
.title,
.description {
  text-align: center;
  margin-bottom: 20px;
}

.input-card {
  max-width: 700px;
  margin: 20px auto;
  padding: 25px;
  border-radius: 12px;
  background-color: #ffffff;
  box-shadow: 0 6px 18px rgba(0, 0, 0, 0.1);
}

.input-description {
  font-size: 15px;
  color: #606060;
  text-align: center;
  margin-bottom: 20px;
}

.collapse-container {
  border-top: 1px solid #dcdfe6;
  padding-top: 10px;
}

.collapse-button {
  background-color: #409eff;
  color: #ffffff;
  border: none;
  padding: 8px 16px;
  border-radius: 6px;
  cursor: pointer;
  font-size: 14px;
  transition: background-color 0.3s ease;
  margin-bottom: 10px;
}

.collapse-button:hover {
  background-color: #66b1ff;
}


.button-group {
  text-align: right;
  margin-bottom: 10px;
}

.custom-textarea {
  width: 100%;
  min-height: 150px;
  background-color: #f5f7fa;
  border: 2px solid #dcdfe6;
  border-radius: 8px;
  padding: 12px;
  font-size: 16px;
  line-height: 1.6;
  resize: vertical;
  transition: border-color 0.3s ease, box-shadow 0.3s ease;
  outline: none;
}

.custom-textarea:focus {
  border-color: #409eff;
  box-shadow: 0 0 8px rgba(64, 158, 255, 0.5);
}

.action-buttons {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 15px;
  margin-bottom: 25px;
}

.action-btn {
  padding: 8px 16px;
  color: #fff;
  background-color: #409eff;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  font-size: 15px;
  transition: background-color 0.3s ease, transform 0.2s ease;
}

.action-btn:hover {
  background-color: #66b1ff;
  transform: translateY(-2px);
}

.styled-table {
  width: 100%;
  border-collapse: collapse;
  margin: 0 auto;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.1);
}

.styled-table th,
.styled-table td {
  padding: 14px 20px;
  text-align: left;
  border-bottom: 1px solid #e0e0e0;
}

.styled-table th {
  background-color: #409eff;
  color: #ffffff;
  font-weight: 600;
}

.styled-table tbody tr:nth-child(even) {
  background-color: #f9f9f9;
}

.styled-table tbody tr:hover {
  background-color: #f1f1f1;
}

.loading,
.error {
  text-align: center;
  margin-top: 25px;
  font-size: 16px;
}

.error {
  color: #ff4d4f;
}

.loading {
  color: #409eff;
}

/* 自定义弹出提示框样式 */
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background-color: rgba(0, 0, 0, 0.5);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 1000;
}

.modal-content {
  background-color: #ffffff;
  padding: 20px 30px;
  border-radius: 8px;
  text-align: center;
  max-width: 400px;
  width: 90%;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.3);
}

.modal-content h3 {
  margin-bottom: 15px;
  color: #ff4d4f;
}

.modal-content p {
  margin-bottom: 20px;
  font-size: 16px;
  color: #333333;
}

.modal-button {
  background-color: #409eff;
  color: #ffffff;
  border: none;
  padding: 8px 20px;
  border-radius: 6px;
  cursor: pointer;
  font-size: 14px;
  transition: background-color 0.3s ease;
}

.modal-button:hover {
  background-color: #66b1ff;
}
</style>
