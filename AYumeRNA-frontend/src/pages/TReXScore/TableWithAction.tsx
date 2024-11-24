import { defineComponent, type PropType } from 'vue'
import { STable } from '@shene/table'
import ActionLink from './ActionLink'
import type { SortOrder } from '@shene/table/dist/src/types/table'

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
          (a.trexScore || 0) - (b.trexScore || 0),
        sortDirections: ['ascend', 'descend'] as unknown as SortOrder[],
      },
      {
        title: 'Action',
        key: 'action',
        width: 180,
        customRender: ({ record }: { record: TableRow }) => {
          console.log('Rendering action link for sequence:', record.sequence)

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
        rowKey="sequence"
      />
    )
  },
})

export default TableWithAction
