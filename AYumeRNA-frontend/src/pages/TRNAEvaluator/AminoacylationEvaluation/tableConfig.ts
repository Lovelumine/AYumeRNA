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
  {
    title: 'Sequence',
    resizable: true,
    dataIndex: 'sequence',
    key: 'sequence',
    width: 180,
    ellipsis: true
  },
  {
    title: 'T-stem Sequence',
    resizable: true,
    dataIndex: 'tstemSequence',
    key: 'tstemSequence',
    width: 140,
    ellipsis: true
  },
  {
    title: 'T-stem Position',
    resizable: true,
    dataIndex: 'tstemPosition',
    key: 'tstemPosition',
    width: 120,
    ellipsis: true
  },
  {
    title: 'Base Pairs',
    resizable: true,
    dataIndex: 'basePairs',
    key: 'basePairs',
    width: 120,
    ellipsis: true
  },
  {
    title: 'Free Energy (kcal/mol)',
    resizable: true,
    dataIndex: 'freeEnergy',
    key: 'freeEnergy',
    width: 180,
    ellipsis: true
  },
  {
    title: 'Total Free Energy (kcal/mol)',
    resizable: true,
    dataIndex: 'totalFreeEnergy',
    key: 'totalFreeEnergy',
    width: 250,
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
        const totalFreeEnergy = record.totalFreeEnergy;
        return Math.abs(totalFreeEnergy) < value; // 筛选出绝对值小于输入值的记录
      }
    },
    customRender: ({ record }: { record: SequenceData }) => {
      // 如果 freeEnergy 为空或包含错误提示（例如 "poor"），或者 totalFreeEnergy 仍为默认值0，则显示 "N/a"
      if (!record.freeEnergy || record.freeEnergy.includes('poor') || record.totalFreeEnergy === 0) {
        return 'N/a';
      }
      return record.totalFreeEnergy.toFixed(2);
    },
    sorter: (a: SequenceData, b: SequenceData) => a.totalFreeEnergy - b.totalFreeEnergy,
    sortDirections: ['ascend', 'descend'] as unknown as SortOrder[],
  },
  {
    title: 'Actions',
    resizable: true,
    dataIndex: 'actions',
    key: 'actions',
    width: 130
  }
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
