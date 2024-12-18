// tableConfig.ts
import type { STableColumnsType, STablePaginationConfig, STableProps } from '@shene/table'

// 表格列配置
export const columns: STableColumnsType<SequenceData> = [
  { title: 'Sequence', dataIndex: 'sequence', key: 'sequence', width: 200, ellipsis: true },
  { title: 'T-stem Sequence', dataIndex: 'tstemSequence', key: 'tstemSequence', width: 80, ellipsis: true },
  { title: 'T-stem Position', dataIndex: 'tstemPosition', key: 'tstemPosition', width: 80, ellipsis: true },
  { title: 'Base Pairs', key: 'basePairs', width: 80, ellipsis: true },
  { title: 'Free Energy (kcal/mol)', key: 'freeEnergy', width: 150, ellipsis: true },
  { title: 'Total Free Energy (kcal/mol)', key: 'totalFreeEnergy', width: 150, ellipsis: true },
  { title: 'Actions', key: 'actions', width: 100 }
]

// 类型定义
export interface SequenceData {
  key: string
  sequence: string
  tstemSequence: string
  tstemPosition: string
  basePairs: string[]
  freeEnergy: string
}

// 分页配置
export const pagination: STablePaginationConfig = {
  defaultCurrent: 1,
  defaultPageSize: 5,
  showQuickJumper: true,
  showSizeChanger: true,
  showTotal: total => `Total ${total} items`,
  pageSizeOptions: ['5', '10', '20', '50']
}

// 分页事件
export const onPaginationChange: STableProps['onPaginationChange'] = params => {
  console.log('分页参数:', params)
}
