import { createRouter, createWebHistory } from 'vue-router'
import Home from '../pages/HomePage/HomePage.vue'
import CodonGenerator from '../pages/CondonGenerator/CondonGenerator.vue'
import ResultAnalysis from '../pages/ResultAnalysis/ResultAnalysis.vue'
import TReXScore from '../pages/ResultAnalysis/TReXScore/TReXScore.vue' // tREX评分组件
import SecondaryStructure from '../pages/ResultAnalysis/SecondaryStructure/SecondaryStructure.vue' // 二级结构预测组件
import TertiaryStructure from '../pages/ResultAnalysis/TertiaryStructure/TertiaryStructure.vue' // 三级结构预测组件

const routes = [
  { path: '/', name: 'Home', component: Home },
  { path: '/generator', name: 'CodonGenerator', component: CodonGenerator },
  {
    path: '/analysis',
    name: 'ResultAnalysis',
    component: ResultAnalysis,
    children: [
      { path: 'trex-score', name: 'TReXScore', component: TReXScore },
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
