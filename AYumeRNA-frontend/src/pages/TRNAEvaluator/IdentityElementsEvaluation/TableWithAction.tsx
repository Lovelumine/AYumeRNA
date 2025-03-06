import { defineComponent, ref, computed, type PropType, nextTick } from 'vue'
import { defineExpose } from 'vue'
import type { SortOrder } from '@shene/table/dist/src/types/table'
import STable, { STableProvider } from '@shene/table'
import { ElInputNumber } from 'element-plus'
import en from '@shene/table/dist/locale/en'
import { useRouter } from 'vue-router'

import ActionLink from './ActionLink'
import { defaultRowSelection } from './RowSelection'
import styles from './TableWithAction.module.css'

// 定义表格行数据接口
export interface TableRow {
  sequence: string
  trexScore: number | null
}

// 定义表格列
const displayedColumns = [
  {
    title: 'Sequence',
    dataIndex: 'sequence',
    key: 'sequence',
    width: 700,
    ellipsis: true,
    className: 'sequence-column',
    resizable: true,
  },
  {
    title: 'tREX Score',
    dataIndex: 'trexScore',
    key: 'trexScore',
    width: 200,
    ellipsis: true,
    className: 'trex-score-column',
    resizable: true,
    filter: {
      component: ElInputNumber,
      props: {
        placeholder: 'Enter minimum Score',
        style: { width: '150px' },
        min: 0,
        step: 0.1,
        controls: false,
      },
      onFilter: (value: number | null, record: TableRow) => {
        if (value == null) return true
        return (record.trexScore ?? 0) >= value
      },
    },
    customRender: ({ record }: { record: TableRow }) => {
      return record.trexScore === null ? 'Waiting' : record.trexScore.toFixed(2)
    },
    sorter: (a: TableRow, b: TableRow) => (a.trexScore || 0) - (b.trexScore || 0),
    sortDirections: ['ascend', 'descend'] as SortOrder[],
  },
  {
    title: 'Action',
    key: 'action',
    width: 180,
    customRender: ({ record }: { record: TableRow }) => {
      return <ActionLink sequence={record.sequence} />
    },
    className: 'action-column',
    resizable: true,
  },
]

export default defineComponent({
  name: 'TableWithAction',
  props: {
    dataSource: {
      type: Array as PropType<TableRow[]>,
      required: true,
    },
  },
  setup(props) {
    const router = useRouter()

    // 用于引用 <STable> 实例
    const tableRef = ref<unknown>(null)

    // 选中的行数据
    const selectedRowKeys = ref<(string | number)[]>([])
    const selectedRows = ref<TableRow[]>([])

    // 是否有选中行
    const isSelected = computed(() => selectedRows.value.length > 0)

    // 全选所有行
    const handleSelectAll = async () => {
      selectedRowKeys.value = props.dataSource.map(item => item.sequence)
      selectedRows.value = [...props.dataSource]
      console.log('SelectAll: selected keys:', selectedRowKeys.value)

      // 等待下一次 DOM 更新后强制刷新表格的选中状态
      await nextTick()
      if (tableRef.value) {
        // 如果你的 <STable> 有类似 updateSelectedRows 或 updateSelection 之类的方法，则在此调用
        // 否则可忽略
        // tableRef.value.updateSelectedRows(selectedRows.value)
      }
    }

    // 清空所有选择
    const handleClearSelection = async () => {
      selectedRowKeys.value = []
      selectedRows.value = []
      console.log('ClearSelection: all selections cleared')

      await nextTick()
      if (tableRef.value) {
        // tableRef.value.clearSelection()
      }
    }

    // 选择 tREX Score ≥ 0.2 的行
    const handleSelectTREXScore = async () => {
      const filteredRows = props.dataSource.filter(row => (row.trexScore ?? 0) >= 0.2)
      selectedRowKeys.value = filteredRows.map(row => row.sequence)
      selectedRows.value = filteredRows
      console.log('Selected rows with tREX Score ≥ 0.2:', selectedRowKeys.value)

      await nextTick()
      if (tableRef.value) {
        // tableRef.value.updateSelectedRows(filteredRows)
      }
    }

    // 自定义弹窗
    const showCustomModal = ref(false)
    const modalMessage = ref('')

    const openModal = (msg: string) => {
      modalMessage.value = msg
      showCustomModal.value = true
    }
    const closeModal = () => {
      showCustomModal.value = false
    }

    // 下载选中行的 CSV
    const downloadSelectedSequences = () => {
      if (!isSelected.value) {
        openModal('Please select at least one sequence before downloading.')
        return
      }
      const headers = ['Sequence', 'tREX Score']
      const csvContent = [
        headers.join(','),
        ...selectedRows.value.map(row => `${row.sequence},"${row.trexScore ?? 'N/A'}"`),
      ].join('\n')

      const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' })
      const url = URL.createObjectURL(blob)
      const link = document.createElement('a')
      link.setAttribute('href', url)
      link.setAttribute('download', 'selected_sequences.csv')
      document.body.appendChild(link)
      link.click()
      document.body.removeChild(link)
    }

    // 导航到新页面
    const navigateToAffinityElements = () => {
      if (!isSelected.value) {
        openModal('Please select at least one sequence before proceeding.')
        return
      }
      localStorage.setItem('cached_sequences_after_steptwo', JSON.stringify(selectedRows.value))
      router.push('/trna-evaluator/aminoacylation').then(() => {
        console.log('Navigated to /trna-evaluator/aminoacylation')
      })
    }

    // 使用 defineExpose 将方法暴露给父组件
    defineExpose({
      handleSelectAll,
      handleClearSelection,
      handleSelectTREXScore,
    })

    // 返回对象，render 函数渲染组件
    return {
      tableRef,
      selectedRowKeys,
      selectedRows,
      isSelected,
      handleSelectAll,
      handleClearSelection,
      handleSelectTREXScore,
      downloadSelectedSequences,
      navigateToAffinityElements,
      openModal,
      closeModal,
      showCustomModal,
      modalMessage,

      render: () => (
        <div>
          {/* 操作按钮区域 */}
          <div class={styles.actionButtons}>
            <button class={styles['collapse-button']} onClick={handleSelectAll}>
              SelectAll
            </button>
            <button class={styles['collapse-button']} onClick={handleClearSelection}>
              Clear Selection
            </button>
            <button class={`${styles['collapse-button']} ${styles['red-button']}`} onClick={handleSelectTREXScore}>
              Select tREX Score ≥ 0.2
            </button>
            <button
              class={styles['collapse-button']}
              onClick={downloadSelectedSequences}
              style={{
                backgroundColor: isSelected.value ? 'green' : '#c0c4cc',
                borderColor: isSelected.value ? 'green' : '#c0c4cc',
                cursor: 'pointer',
              }}
            >
              Download Selected Sequences
            </button>
            <button
              class={styles['collapse-button']}
              onClick={navigateToAffinityElements}
              style={{
                backgroundColor: isSelected.value ? 'green' : '#c0c4cc',
                borderColor: isSelected.value ? 'green' : '#c0c4cc',
                cursor: 'pointer',
              }}
            >
              Navigate to The affinity between aa-tRNAs and release factor
            </button>
          </div>

          {/* 表格区域 */}
          <STableProvider locale={en}>
            <STable
              // 绑定引用，用于在 handleSelectAll 中调用
              ref={tableRef}
              v-model:selectedRowKeys={selectedRowKeys.value}
              columns={displayedColumns}
              dataSource={props.dataSource}
              row-selection={{
                ...defaultRowSelection,
                selectedRowKeys: selectedRowKeys.value,
                onChange: (keys: (string | number)[], rows: TableRow[]) => {
                  console.log('onChange: keys:', keys, 'rows:', rows)
                  selectedRowKeys.value = keys
                  selectedRows.value = rows
                },
              }}
              highlight-selected
              stripe
              size="default"
              showSorterTooltip
              bordered
              hover
              pagination={{ pageSize: 5 }}
              rowKey="sequence"
            />
          </STableProvider>

          {/* 自定义弹出提示框 */}
          {showCustomModal.value && (
            <div class={styles.modalOverlay} onClick={closeModal}>
              <div class={styles.modalContent} onClick={(e: MouseEvent) => e.stopPropagation()}>
                <h3>Warning</h3>
                <p>{modalMessage.value}</p>
                <button class={styles.modalButton} onClick={closeModal}>OK</button>
              </div>
            </div>
          )}
        </div>
      ),
    }
  },

  // 渲染函数
  render() {
    return this.render()
  },
})
