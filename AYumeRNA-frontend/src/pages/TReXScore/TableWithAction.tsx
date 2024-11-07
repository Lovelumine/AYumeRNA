import { defineComponent, type PropType } from 'vue'
import { STable } from '@shene/table'
import ActionLink from './ActionLink'
import type { SortOrder } from '@shene/table/dist/src/types/table'

// 定义数据行类型
interface TableRow {
  sequence: string
  trexScore: number
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
        sorter: (a: TableRow, b: TableRow) => a.trexScore - b.trexScore, // 添加排序功能
        sortDirections: ['ascend', 'descend'] as unknown as SortOrder[], // 使用 `as unknown as SortOrder[]` 强制转换
      },
      {
        title: 'Action',
        key: 'action',
        width: 180,
        customRender: () => {
          // 渲染 ActionLink 组件
          return <ActionLink />
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
