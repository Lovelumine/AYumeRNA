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
  },  { title: 'Actions', key: 'actions', width: 100 }
]
