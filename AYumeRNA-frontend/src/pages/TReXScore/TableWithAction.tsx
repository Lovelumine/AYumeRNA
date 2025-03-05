import { defineComponent, ref, type PropType } from 'vue'
import { STable, STableProvider } from '@shene/table'
import ActionLink from './ActionLink'
import en from '@shene/table/dist/locale/en'
import type { SortOrder } from '@shene/table/dist/src/types/table'
import styles from './TableWithAction.module.css'

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
  emits: ['download-selected'],
  setup(props, { emit }) {
    const selectedRowKeys = ref<(string | number)[]>([]) // 用于存储选中的行键
    const selectedRows = ref<TableRow[]>([]) // 存储选中行的完整数据

    // 自定义行点击行为
    const customRow = ({ record }: { record: TableRow }) => {
      if (!record) return {}
      return {
        onClick: () => {
          const index = selectedRowKeys.value.indexOf(record.sequence)
          if (index > -1) {
            selectedRowKeys.value.splice(index, 1)
          } else {
            selectedRowKeys.value.push(record.sequence)
          }
          console.log('Updated selectedRowKeys:', selectedRowKeys.value)
        },
        style: { cursor: 'pointer' },
      }
    }

    // 定义表格列
    const displayedColumns = [
      {
        title: 'Sequence',
        dataIndex: 'sequence',
        key: 'sequence',
        width: 800,
        ellipsis: true,
        className: 'sequence-column',
        resizable: true,
      },
      {
        title: 'tREX Score',
        dataIndex: 'trexScore',
        key: 'trexScore',
        width: 120,
        ellipsis: true,
        className: 'trex-score-column',
        resizable: true,
        customRender: ({ record }: { record: TableRow }) => {
          const { trexScore } = record
          return trexScore === null ? 'Waiting' : trexScore.toFixed(2)
        },
        sorter: (a: TableRow, b: TableRow) =>
          (a.trexScore || 0) - (b.trexScore || 0),
        sortDirections: ['ascend', 'descend'] as unknown as SortOrder[],
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

    return () => (
      <div>
        <button
          class={styles.downloadBtn}
          onClick={() => emit('download-selected', selectedRows.value)}
        >
          Download Selected Rows
        </button>

        <STableProvider locale={en}>
          <STable
            v-model:selectedRowKeys={selectedRowKeys.value}
            columns={displayedColumns}
            dataSource={props.dataSource}
            customRow={customRow}
            row-selection="rowSelection"
            highlight-selected
            stripe
            size="default"
            showSorterTooltip
            bordered
            hover
            pagination={{ pageSize: 5 }}
            rowSelection={{
              type: 'checkbox',
              onChange: (keys: (string | number)[], rows: TableRow[]) => {
                selectedRowKeys.value = keys
                selectedRows.value = rows
                console.log('Selected rows updated:', rows)
              },
            }}
            rowKey="sequence"
          />
        </STableProvider>
      </div>
    )
  },
})

export default TableWithAction
