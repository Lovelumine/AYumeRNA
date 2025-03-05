// src/pages/TRNAEvaluator/IdentityElementsEvaluation/TableWithAction.tsx

import { ElInputNumber } from 'element-plus'
import { defineComponent, ref, type PropType, computed } from 'vue'
import type { SortOrder } from '@shene/table/dist/src/types/table'
import STable, { STableProvider } from '@shene/table'
import ActionLink from './ActionLink'
import en from '@shene/table/dist/locale/en'
import styles from './TableWithAction.module.css'
import { useRouter } from 'vue-router'
import { defaultRowSelection } from './RowSelection'

// 定义表格行数据接口
export interface TableRow {
  sequence: string
  trexScore: number | null
}

// 定义表格列（针对 tREX Score 进行设置）
const displayedColumns = [
  {
    title: 'Sequence',
    dataIndex: 'sequence',
    key: 'sequence',
    width: 700,
    ellipsis: true,
    className: 'sequence-column',
    resizable: true,
  },
  {
    title: 'tREX Score',
    dataIndex: 'trexScore',
    key: 'trexScore',
    width: 200,
    ellipsis: true,
    className: 'trex-score-column',
    resizable: true,
    filter: {
      component: ElInputNumber, // 使用 ElInputNumber 组件
      props: {
        placeholder: 'Enter minimum Score',
        style: { width: '150px' },
        min: 0,
        step: 0.1,
        controls: false,
      },
      onFilter: (value: number | null, record: TableRow) => {
        if (value == null) return true;
        const score = record.trexScore ?? 0;
        return score >= value;
      },
    },
    customRender: ({ record }: { record: TableRow }) => {
      const { trexScore } = record;
      return trexScore === null ? 'Waiting' : trexScore.toFixed(2);
    },
    sorter: (a: TableRow, b: TableRow) =>
      (a.trexScore || 0) - (b.trexScore || 0),
    sortDirections: ['ascend', 'descend'] as SortOrder[],
  },
  {
    title: 'Action',
    key: 'action',
    width: 180,
    customRender: ({ record }: { record: TableRow }) => {
      return <ActionLink sequence={record.sequence} />
    },
    className: 'action-column',
    resizable: true,
  },
];

export default defineComponent({
  name: 'TableWithAction',
  props: {
    dataSource: {
      type: Array as PropType<TableRow[]>,
      required: true,
    },
  },
  setup(props) {
    const router = useRouter();

    // 用于存储选中的行键和行数据
    const selectedRowKeys = ref<(string | number)[]>([]);
    const selectedRows = ref<TableRow[]>([]);

    // 计算是否有选中行
    const isSelected = computed(() => selectedRows.value.length > 0);

    // 全选：将所有行选中（这里假设 rowKey 使用 sequence 字段且唯一）
    const handleSelectAll = () => {
      const allKeys = props.dataSource.map(item => item.sequence);
      selectedRowKeys.value = allKeys;
      selectedRows.value = [...props.dataSource];
      console.log('SelectAll: selected keys:', allKeys);
    };

    // 取消选择：清空所有选中状态
    const handleClearSelection = () => {
      selectedRowKeys.value = [];
      selectedRows.value = [];
      console.log('ClearSelection: all selections cleared');
    };

    // 选择 tREX Score ≥ 0.2 的行
    const handleSelectTREXScore = () => {
      const filteredRows = props.dataSource.filter(row => {
        let score = row.trexScore;
        if (typeof score === 'string') {
          score = parseFloat(score);
        }
        return typeof score === 'number' && score >= 0.2;
      });
      selectedRowKeys.value = filteredRows.map(row => row.sequence);
      selectedRows.value = filteredRows;
      console.log('Selected rows with tREX Score ≥ 0.2:', selectedRowKeys.value);
    };

    // 定义 rowSelection 配置
    const rowSelection = computed(() => ({
      // 使用 checkbox 多选
      ...defaultRowSelection,
      type: 'checkbox',
      selectedRowKeys: selectedRowKeys.value,
      onChange: (keys: (string | number)[], rows: TableRow[]) => {
        console.log('onChange: keys:', keys, 'rows:', rows);
        selectedRowKeys.value = keys;
        selectedRows.value = rows;
      },
    }));

    // 下载选中行的 CSV
    const downloadSelectedSequences = () => {
      if (selectedRows.value.length === 0) {
        // 如果没有选中行，则显示弹出提示框
        showCustomModal.value = true;
        return;
      }
      const headers = ['Sequence', 'tREX Score'];
      const csvContent = [
        headers.join(','),
        ...selectedRows.value.map(row =>
          `${row.sequence},"${row.trexScore ?? 'N/A'}"`
        ),
      ].join('\n');
      const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' });
      const url = URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.setAttribute('href', url);
      link.setAttribute('download', 'selected_sequences.csv');
      link.style.visibility = 'hidden';
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
    };

    // 导航：保存选中行并跳转页面
    const navigateToAffinityElements = () => {
      if (selectedRows.value.length === 0) {
        showCustomModal.value = true;
        return;
      }
      localStorage.setItem('cached_sequences_after_steptwo', JSON.stringify(selectedRows.value));
      router.push('/trna-evaluator/aminoacylation').then(() => {
        console.log('Navigated to /trna-evaluator/aminoacylation');
      });
    };

    // 弹出提示框控制
    const showCustomModal = ref(false);
    const closeModal = () => {
      showCustomModal.value = false;
    };

    return () => (
      <div>
        {/* 操作按钮区域 */}
        <div class={styles.actionButtons}>
          <button class={styles['collapse-button']} onClick={handleSelectAll}>
            SelectAll
          </button>
          <button class={styles['collapse-button']} onClick={handleClearSelection}>
            Clear Selection
          </button>
          <button class={styles['collapse-button']} onClick={handleSelectTREXScore}>
            Select tREX Score ≥ 0.2
          </button>
          <button
            class={styles.downloadBtn}
            onClick={downloadSelectedSequences}
            disabled={selectedRows.value.length === 0}
            style={{
              backgroundColor: isSelected.value ? 'green' : '#c0c4cc',
              borderColor: isSelected.value ? 'green' : '#c0c4cc',
            }}
          >
            Download Selected Sequences
          </button>
          <button
            class={styles.navigateBtn}
            onClick={navigateToAffinityElements}
            disabled={selectedRows.value.length === 0}
            style={{
              backgroundColor: isSelected.value ? 'green' : '#c0c4cc',
              borderColor: isSelected.value ? 'green' : '#c0c4cc',
            }}
          >
            Navigate to The affinity between aa-tRNAs and release factor
          </button>
        </div>

        {/* 表格区域 */}
        <STableProvider locale={en}>
          <STable
            v-model:selectedRowKeys={selectedRowKeys.value}
            columns={displayedColumns}
            dataSource={props.dataSource}
            row-selection={rowSelection.value}
            highlight-selected
            stripe
            size="default"
            showSorterTooltip
            bordered
            hover
            pagination={{ pageSize: 5 }}
            rowKey="sequence"
          />
        </STableProvider>

        {/* 自定义弹出提示框 */}
        {showCustomModal.value && (
          <div class={styles.modalOverlay} onClick={() => closeModal()}>
            <div class={styles.modalContent} onClick={(e: MouseEvent) => e.stopPropagation()}>
              <h3>Warning</h3>
              <p>Please select at least one sequence first.</p>
              <button class={styles.modalButton} onClick={closeModal}>
                OK
              </button>
            </div>
          </div>
        )}
      </div>
    );
  },
});
