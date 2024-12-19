import { ElInputNumber } from 'element-plus'
import { defineComponent, ref, type PropType } from 'vue'
import type { SortOrder } from '@shene/table/dist/src/types/table'
import STable, { STableProvider } from '@shene/table'
import ActionLink from './ActionLink'
import en from '@shene/table/dist/locale/en'
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
