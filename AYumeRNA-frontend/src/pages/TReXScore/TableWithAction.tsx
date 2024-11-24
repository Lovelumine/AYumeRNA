import { defineComponent, type PropType } from 'vue'
import { STable } from '@shene/table'
import ActionLink from './ActionLink'
import type { SortOrder } from '@shene/table/dist/src/types/table'

// 定义数据行类型
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
    console.log('TableWithAction - Received dataSource:', props.dataSource)

    // 定义表格列
    const displayedColumns = [
      {
        title: 'Sequence',
        dataIndex: 'sequence',
        key: 'sequence',
        width: 200,
        ellipsis: true,
        className: 'sequence-column',
      },
      {
        title: 'tREX Score',
        dataIndex: 'trexScore',
        key: 'trexScore',
        width: 120,
        ellipsis: true,
        className: 'trex-score-column',
        customRender: ({ record }: { record: TableRow }) => {
          const { trexScore } = record
          console.log('Rendering tREX score for:', record)
          return trexScore === null ? 'Not Calculated' : trexScore.toFixed(2)
        },
        sorter: (a: TableRow, b: TableRow) =>
          (a.trexScore || 0) - (b.trexScore || 0), // 添加排序功能
        sortDirections: ['ascend', 'descend'] as unknown as SortOrder[], // 使用 `as unknown as SortOrder[]` 强制转换
      },
      {
        title: 'Action',
        key: 'action',
        width: 180,
        customRender: ({ record }: { record: TableRow }) => {
          console.log('Rendering action link for sequence:', record.sequence)

          // 渲染 ActionLink 组件，并传递序列信息
          return <ActionLink sequence={record.sequence} />
        },
        className: 'action-column',
      },
    ]

    return () => (
      <STable
        columns={displayedColumns}
        dataSource={props.dataSource}
        stripe
        size="default"
        showSorterTooltip
        bordered
        hover
        pagination={{ pageSize: 5 }}
        rowSelection={{}}
        rowKey="sequence" // 添加 rowKey，使用 sequence 作为唯一标识
      />
    )
  },
})

export default TableWithAction
