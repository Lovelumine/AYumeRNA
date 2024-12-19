<template>
  <div class="structure-folding-evaluation">
    <h3 class="title">Structure Folding Evaluation</h3>
    <p class="description">
      The table below displays the structure folding parameters of each sequence, including anticodon, infernal score, and tRNA positions.
      These parameters are key to understanding the structural features of the tRNA.
    </p>

    <!-- 多选操作按钮 -->
    <div class="action-buttons">
      <ElButton type="primary" @click="downloadSelectedSequences" :disabled="selectedRows.length === 0">
        Download Selected Sequences
      </ElButton>
      <ElButton type="success" @click="navigateToIdentityElements" :disabled="selectedRows.length === 0">
        Navigate to Identity Elements
      </ElButton>
    </div>

    <!-- 使用 shane 表格展示 -->
    <s-table-provider :locale="en">
      <s-table
        :columns="columns"
        :data-source="dataSource"
        :max-height="300"
        row-key="key"
        :row-selection="rowSelection"
        :highlight-selected="highlightSelected"
      >
        <template #bodyCell="{ text, column, record }">
          <template v-if="column.key === 'sequence'">
            <a>{{ text }}</a>
          </template>
          <template v-else-if="column.key === 'anticodon'">
            {{ record.anticodon }}
          </template>
          <template v-else-if="column.key === 'infernalScore'">
            {{ record.infernalScore }}
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

    <!-- 加载和错误提示 -->
    <div v-if="loading" class="loading">Loading...</div>
    <div v-if="error" class="error">{{ error }}</div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { ElButton } from 'element-plus'
import en from '@shene/table/dist/locale/en'
import { columns, defaultRowSelection } from './tableConfig'
import type { Key } from './tableConfig' // Type-only import
import {
  dataSource,
  loading,
  error,
  loadSequences,
  handleAnalyzeSequence
} from './logic'
import { useRouter } from 'vue-router'
import type { STableRowSelection } from '@shene/table'
import type { SequenceInfo } from './tableConfig'

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

// 定义跳转到 Identity Elements 页面并存储选中序列的函数
const navigateToIdentityElements = () => {
  if (selectedRows.value.length === 0) return

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
    console.log('Navigated to /trna-evaluator/identity-elements')
  })
}

// 加载数据
loadSequences()
</script>

<style scoped>
.title, .description {
  text-align: center;
}

.action-buttons {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 10px;
  margin-bottom: 20px;
}

.loading, .error {
  text-align: center;
  margin-top: 20px;
}

.error {
  color: red;
}

.loading {
  color: #409eff;
}

.action-btn {
  padding: 4px 8px;
  color: #fff;
  background-color: #409eff;
  border: none;
  border-radius: 4px;
  cursor: pointer;
}

.action-btn:hover {
  background-color: #66b1ff;
}
</style>
