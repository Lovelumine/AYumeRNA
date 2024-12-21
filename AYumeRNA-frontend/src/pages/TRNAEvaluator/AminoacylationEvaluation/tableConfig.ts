// src/pages/TRNAEvaluator/AminoacylationEvaluation/tableConfig.ts
import type { STableColumnsType, STablePaginationConfig, STableProps } from '@shene/table'
import type { SortOrder } from '@shene/table/dist/src/types/table'
import { ElInputNumber } from 'element-plus'

// 类型定义
export interface SequenceData {
  key: string
  sequence: string
  tstemSequence: string
  tstemPosition: string
  basePairs: string[]
  freeEnergy: string
  totalFreeEnergy: number // 新增属性
}

// 表格列配置
export const columns: STableColumnsType<SequenceData> = [
  { title: 'Sequence', dataIndex: 'sequence', key: 'sequence', width: 200, ellipsis: true },
  { title: 'T-stem Sequence', dataIndex: 'tstemSequence', key: 'tstemSequence', width: 80, ellipsis: true },
  { title: 'T-stem Position', dataIndex: 'tstemPosition', key: 'tstemPosition', width: 80, ellipsis: true },
  { title: 'Base Pairs', dataIndex: 'basePairs', key: 'basePairs', width: 80, ellipsis: true },
  { title: 'Free Energy (kcal/mol)', dataIndex: 'freeEnergy', key: 'freeEnergy', width: 150, ellipsis: true },
  {
    title: 'Total Free Energy (kcal/mol)',
    dataIndex: 'totalFreeEnergy',
    key: 'totalFreeEnergy',
    width: 150,
    ellipsis: true,
    filter: {
      component: ElInputNumber,  // 使用 ElInputNumber 组件
      props: {
        placeholder: 'Enter max absolute energy', // 英文占位符
        style: { width: '150px' },
        min: 0,  // 设置最小值为 0
        step: 0.1, // 设置步进值为 0.1
        controls: false, // 隐藏增减按钮
      },
      onFilter: (value: number | null, record: SequenceData) => {
        if (value == null) return true;  // 如果没有输入值则不过滤
        const totalFreeEnergy = record.totalFreeEnergy
        return Math.abs(totalFreeEnergy) < value // 筛选出绝对值小于输入值的记录
      }
    },
    customRender: ({ record }: { record: SequenceData }) => {
      // 安全检查，确保 totalFreeEnergy 是有效数字
      if (typeof record.totalFreeEnergy === 'number' && !isNaN(record.totalFreeEnergy)) {
        return record.totalFreeEnergy.toFixed(2)
      }
      return 'Please select Amino Acid'
    },
    sorter: (a: SequenceData, b: SequenceData) => a.totalFreeEnergy - b.totalFreeEnergy,
    sortDirections: ['ascend', 'descend'] as unknown as SortOrder[],
  },
  { title: 'Actions', dataIndex: 'actions', key: 'actions', width: 100 }
]

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
