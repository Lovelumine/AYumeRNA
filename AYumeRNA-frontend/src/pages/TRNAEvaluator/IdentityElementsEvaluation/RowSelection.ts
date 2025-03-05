import type { STableRowSelection } from '@shene/table'
import type { TableRow } from './TableWithAction' // 确保 TableRow 结构一致

export const defaultRowSelection: STableRowSelection<TableRow> = {
  hideSelectAll: true, // 隐藏全选框
  onChange: (selectedRowKeys: (string | number)[], selectedRows: TableRow[]) => {
    console.log('onChange selectedRowKeys:', selectedRowKeys)
    console.log('onChange selectedRows:', selectedRows)
  },
  getCheckboxProps: (record: TableRow) => ({
    disabled: record.sequence === '特定序列',
    name: record.sequence
  })
}
