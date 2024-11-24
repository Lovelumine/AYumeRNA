import { defineComponent } from 'vue'
import { useRouter } from 'vue-router'

const ActionLink = defineComponent({
  name: 'ActionLink',
  props: {
    sequence: {
      type: String,
      required: true,
    },
  },
  setup(props) {
    const router = useRouter() // 使用 Vue Router

    const handleClick = (event: Event) => {
      event.preventDefault() // 防止默认的超链接行为
      console.log('VisualizationAnalysis clicked for sequence:', props.sequence)

      // 将序列保存到本地存储中
      localStorage.setItem(
        'analyzedSequence',
        JSON.stringify({ sequence: props.sequence }),
      )
      console.log('Sequence saved for analysis:', props.sequence)

      // 跳转到 /visualization
      router.push('/visualization').then(() => {
        console.log('Navigated to /visualization')
      })
    }

    return () => (
      <a href="#" class="link" onClick={handleClick}>
        VisualizationAnalysis
      </a>
    )
  },
})

export default ActionLink
