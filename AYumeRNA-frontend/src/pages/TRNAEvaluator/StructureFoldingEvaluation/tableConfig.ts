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
    width: 200,
  },
  {
    title: 'Anticodon',
    dataIndex: 'anticodon',
    key: 'anticodon',
    width: 180,
  },
  {
    title: 'Infernal Score',
    dataIndex: 'infernalScore',
    key: 'infernalScore',
    width: 180,
  },
  {
    title: 'tRNA Begin',
    dataIndex: 'tRNAStart',
    key: 'tRNAStart',
    width: 180,
  },
  {
    title: 'tRNA End',
    dataIndex: 'tRNAEnd',
    key: 'tRNAEnd',
    width: 180,
  },
  {
    title: 'tRNA Type',
    dataIndex: 'tRNAType',
    key: 'tRNAType',
    width: 180,
  },
]
