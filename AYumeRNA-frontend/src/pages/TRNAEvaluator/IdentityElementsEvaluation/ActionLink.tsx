import { defineComponent } from 'vue'
import { ElButton } from 'element-plus'  // 引入 Element Plus 按钮组件

const ActionLink = defineComponent({
  name: 'ActionLink',
  props: {
    sequence: {
      type: String,
      required: true,
    },
  },
  setup(props) {
    const handleClick = (event: Event) => {
      event.preventDefault()
      console.log('VisualizationAnalysis clicked for sequence:', props.sequence)

      // 保存到 localStorage
      localStorage.setItem(
        'analyzedSequence',
        JSON.stringify({ sequence: props.sequence }),
      )
      console.log('Sequence saved for analysis:', props.sequence)

      // 使用 window.open 在新页面打开 /visualization
      window.open('/visualization', '_blank')
    }

    return () => (
      <ElButton
        type="primary"  // 主要按钮样式
        size="small"    // 小号按钮
        onClick={handleClick}
        class="action-btn"
      >
        Visualization
      </ElButton>
    )
  },
})

export default ActionLink
