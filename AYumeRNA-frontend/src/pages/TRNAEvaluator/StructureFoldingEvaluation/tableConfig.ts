import { ElInputNumber } from 'element-plus'
import type { STableColumnsType } from '@shene/table'

export interface SequenceInfo {
  key: string
  sequence: string
  anticodon: string | null
  infernalScore: string | null
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
      onFilter: (value, record) => {
        if (value == null) return true;  // 如果没有输入值则不过滤
        const score = parseFloat(record.infernalScore || '0'); // 获取记录中的 Infernal Score
        return score >= value;  // 筛选大于等于用户输入的值
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
  { title: 'Actions', key: 'actions', width: 100 }
]
