import { createRouter, createWebHistory } from 'vue-router'
import Home from '../pages/HomePage/HomePage.vue'
import CodonGenerator from '../pages/CondonGenerator/CondonGenerator.vue'
import TReXScore from '../pages/ResultAnalysis/TReXScore/TReXScore.vue' // tREX评分组件
import VisualizationAnalysis from '../pages/ResultAnalysis/VisualizationAnalysis.vue' // 可视化分析组件
import SecondaryStructure from '../pages/ResultAnalysis/SecondaryStructure/SecondaryStructure.vue' // 二级结构预测组件
import TertiaryStructure from '../pages/ResultAnalysis/TertiaryStructure/TertiaryStructure.vue' // 三级结构预测组件

const routes = [
  { path: '/', name: 'Home', component: Home },
  { path: '/generator', name: 'RfamGen', component: CodonGenerator },
  {
    path: '/trex-score',
    name: 'TReXScore',
    component: TReXScore,
  },
  {
    path: '/visualization',
    name: 'VisualizationAnalysis',
    component: VisualizationAnalysis,
    children: [
      {
        path: 'secondary-structure',
        name: 'SecondaryStructure',
        component: SecondaryStructure,
      },
      {
        path: 'tertiary-structure',
        name: 'TertiaryStructure',
        component: TertiaryStructure,
      },
    ],
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

export default router
