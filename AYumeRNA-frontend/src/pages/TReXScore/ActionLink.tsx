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
    const router = useRouter()

    const handleClick = (event: Event) => {
      event.preventDefault()
      console.log('VisualizationAnalysis clicked for sequence:', props.sequence)

      localStorage.setItem(
        'analyzedSequence',
        JSON.stringify({ sequence: props.sequence }),
      )
      console.log('Sequence saved for analysis:', props.sequence)

      router.push('/visualization/secondary-structure').then(() => {
        console.log('Navigated to /visualization/secondary-structure')
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
