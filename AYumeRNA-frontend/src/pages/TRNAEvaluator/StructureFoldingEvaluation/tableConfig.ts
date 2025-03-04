// src/pages/TRNAEvaluator/StructureFoldingEvaluation/tableConfig.ts

import { ElInputNumber } from 'element-plus'
import type { STableColumnsType, STableRowSelection } from '@shene/table'

// 定义 Key 类型
export type Key = string | number

// 定义 Sequence 接口
export interface Sequence {
  key: string
  sequence: string
}

export interface SequenceInfo {
  key: string
  sequence: string
  anticodon: string | null
  infernalScore: number | string | null  // 修改类型
  tRNAStart: string | null
  tRNAEnd: string | null
  tRNAType: string | null
}

export const columns: STableColumnsType<SequenceInfo> = [
  {
    title: 'Sequence',
    dataIndex: 'sequence',
    key: 'sequence',
    ellipsis: true,
    width: 200,
    resizable: true,
  },
  {
    title: 'Anticodon',
    dataIndex: 'anticodon',
    key: 'anticodon',
    ellipsis: true,
    width: 60,
  },
  {
    title: 'Infernal Score',
    dataIndex: 'infernalScore',
    key: 'infernalScore',
    ellipsis: true,
    width: 80,
    filter: {
      component: ElInputNumber,  // 使用 ElInputNumber 组件
      props: {
        placeholder: 'Enter minimum Score',
        style: { width: '150px' },
        min: 0,  // 设置最小值为 0
        step: 1, // 设置步进值为 1
        controls: false, // 隐藏增减按钮
      },
      onFilter: (value: number | null, record: SequenceInfo) => {
        if (value == null) return true;  // 如果没有输入值则不过滤
        if (typeof record.infernalScore === 'number') {
          return record.infernalScore >= value;
        }
        const score = parseFloat(record.infernalScore as string || '0'); // 获取记录中的 Infernal Score
        return !isNaN(score) && score >= value;  // 筛选大于等于用户输入的值
      }
    }
  },
  {
    title: 'tRNA Begin',
    dataIndex: 'tRNAStart',
    ellipsis: true,
    key: 'tRNAStart',
    width: 80,
  },
  {
    title: 'tRNA End',
    dataIndex: 'tRNAEnd',
    key: 'tRNAEnd',
    width: 80,
  },
  {
    title: 'tRNA Type',
    dataIndex: 'tRNAType',
    ellipsis: true,
    key: 'tRNAType',
    width: 80,
  },
  {
    title: 'Actions',
    key: 'actions',
    width: 100
  }
]
interface OnChangeExtra {
  type: string;
  checked: boolean;
}

export const defaultRowSelection: STableRowSelection<SequenceInfo> = {
  hideSelectAll: true, // 根据需要隐藏全选框
  onChange: (
    selectedRowKeys: Key[],
    selectedRows: SequenceInfo[],
    extra?: OnChangeExtra
  ) => {
    console.log('onChange selectedRowKeys:', selectedRowKeys);
    console.log('onChange selectedRows:', selectedRows);
    console.log('onChange extra:', extra);
  },
  getCheckboxProps: (record: SequenceInfo) => ({
    disabled: record.sequence === '特定序列', // 例如，禁用序列为 '特定序列' 的行
    name: record.sequence
  })
};
