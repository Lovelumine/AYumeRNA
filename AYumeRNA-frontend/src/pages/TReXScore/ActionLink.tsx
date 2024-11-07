import { defineComponent } from 'vue'

const ActionLink = defineComponent({
  name: 'ActionLink',
  setup() {
    return () => (
      <a href="#" class="link">
        VisualizationAnalysis
      </a>
    )
  },
})

export default ActionLink
