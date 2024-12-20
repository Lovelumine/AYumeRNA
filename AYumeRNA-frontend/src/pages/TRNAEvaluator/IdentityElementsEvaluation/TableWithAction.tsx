// src/pages/TRNAEvaluator/IdentityElementsEvaluation/TableWithAction.tsx

import { ElInputNumber } from 'element-plus'
import { defineComponent, ref, type PropType, computed } from 'vue'
import type { SortOrder } from '@shene/table/dist/src/types/table'
import STable, { STableProvider } from '@shene/table'
import ActionLink from './ActionLink'
import en from '@shene/table/dist/locale/en'
import styles from './TableWithAction.module.css'
import { useRouter } from 'vue-router' // 导入 useRouter

interface TableRow {
  sequence: string
  trexScore: number | null
}

const TableWithAction = defineComponent({
  name: 'TableWithAction',
  props: {
    dataSource: {
      type: Array as PropType<TableRow[]>,
      required: true,
    },
  },
  setup(props) {
    const router = useRouter() // 初始化 router

    const selectedRowKeys = ref<(string | number)[]>([]) // 用于存储选中的行键
    const selectedRows = ref<TableRow[]>([]) // 存储选中行的完整数据

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
          component: ElInputNumber,  // 使用 ElInputNumber 组件
          props: {
            placeholder: 'Enter minimum Score',
            style: { width: '150px' },
            min: 0,  // 设置最小值为 0
            step: 1, // 设置步进值为 1
            controls: false, // 隐藏增减按钮
          },
          onFilter: (
            value: number | null,
            record: TableRow
          ) => {
            if (value == null) return true;  // 如果没有输入值则不过滤
            const score = record.trexScore ?? 0; // 获取记录中的 tREX Score
            return score >= value;  // 筛选大于等于用户输入的值
          },
        },
        customRender: ({ record }: { record: TableRow }) => {
          const { trexScore } = record
          return trexScore === null ? 'Please select Amino Acid' : trexScore.toFixed(2)
        },
        sorter: (a: TableRow, b: TableRow) =>
          (a.trexScore || 0) - (b.trexScore || 0),
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

    // 定义 rowSelection
    const rowSelection = computed(() => ({
      type: 'checkbox',
      onChange: (keys: (string | number)[], rows: TableRow[]) => {
        selectedRowKeys.value = keys
        selectedRows.value = rows
      },
    }))

    // 定义下载选中序列的函数
    const downloadSelectedSequences = () => {
      if (selectedRows.value.length === 0) return

      // 将选中的序列转换为 CSV 格式
      const headers = ['Sequence', 'tREX Score']
      const csvContent = [
        headers.join(','), // Header row
        ...selectedRows.value.map(row =>
          `${row.sequence},"${row.trexScore ?? 'N/A'}"`
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

    // 定义导航到Affinity Elements页面的函数
    const navigateToAffinityElements = () => {
      if (selectedRows.value.length === 0) return

      // 获取当前时间戳
      const timestamp = new Date().toISOString()

      // 存储时间戳到 localStorage
      localStorage.setItem('timestamp_cached_sequences_after_steptwo', timestamp)

      // 存储选中的序列到 localStorage
      localStorage.setItem('cached_sequences_after_steptwo', JSON.stringify(selectedRows.value))

      // 跳转到 The affinity between aa-tRNAs and release factor 页面
      router.push('/trna-evaluator/aminoacylation').then(() => {
        console.log('Navigated to /trna-evaluator/aminoacylation')
      })
    }

    return () => (
      <div>
        <div class={styles.actionButtons}>
          <button
            class={styles.downloadBtn}
            onClick={downloadSelectedSequences} // 绑定到函数
            disabled={selectedRows.value.length === 0}
          >
            Download Selected Rows
          </button>
          <button
            class={styles.navigateBtn}
            onClick={navigateToAffinityElements}
            disabled={selectedRows.value.length === 0}
          >
            Navigate to The affinity between aa-tRNAs and release factor
          </button>
        </div>

        <STableProvider locale={en}>
          <STable
            v-model:selectedRowKeys={selectedRowKeys.value}
            columns={displayedColumns}
            dataSource={props.dataSource}
            row-selection={rowSelection.value}
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
      </div>
    )
  },
})

export default TableWithAction
