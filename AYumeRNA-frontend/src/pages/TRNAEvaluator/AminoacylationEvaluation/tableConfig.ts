// tableConfig.ts
import type { STableColumnsType, STablePaginationConfig, STableProps } from '@shene/table'

// 表格列配置
export const columns: STableColumnsType<SequenceData> = [
  { title: 'Sequence', dataIndex: 'sequence', key: 'sequence', width: 200, ellipsis: true },
  { title: 'T-stem Sequence', dataIndex: 'tstemSequence', key: 'tstemSequence', width: 80, ellipsis: true },
  { title: 'T-stem Position', dataIndex: 'tstemPosition', key: 'tstemPosition', width: 80, ellipsis: true },
  { title: 'Base Pairs', key: 'basePairs', width: 80, ellipsis: true },
  { title: 'Free Energy (kcal/mol)', key: 'freeEnergy', width: 150, ellipsis: true },
  { title: 'Total Free Energy (kcal/mol)', key: 'totalFreeEnergy', width: 150, ellipsis: true }
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
  defaultCurrent: 1, // 默认当前页
  defaultPageSize: 5, // 默认每页显示5条
  showQuickJumper: true, // 显示快速跳转输入框
  showSizeChanger: true, // 允许改变每页条数
  showTotal: total => `Total ${total} items`, // 显示总条数信息
  pageSizeOptions: ['5', '10', '20', '50'] // 每页可选条数
}

// 分页事件
export const onPaginationChange: STableProps['onPaginationChange'] = params => {
  console.log('分页参数:', params)
}
