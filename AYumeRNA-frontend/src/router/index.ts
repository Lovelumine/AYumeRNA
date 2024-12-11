import { createRouter, createWebHistory } from 'vue-router'
import Home from '../pages/HomePage/HomePage.vue'
import CodonGenerator from '../pages/CondonGenerator/CondonGenerator.vue'
import TReXScore from '../pages/TReXScore/TReXScore.vue' // tREX评分组件
import VisualizationAnalysis from '../pages/ResultAnalysis/VisualizationAnalysis.vue' // 可视化分析组件
import SecondaryStructure from '../pages/ResultAnalysis/SecondaryStructure/SecondaryStructure.vue' // 二级结构预测组件
import TertiaryStructure from '../pages/ResultAnalysis/TertiaryStructure/TertiaryStructure.vue' // 三级结构预测组件
import SequenceVerification from '../pages/SequenceVerification/SequenceVerification.vue' // 序列验证组件

const routes = [
  { path: '/generator', name: 'Sequence Generator', component: CodonGenerator },
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
  {
    path: '/sequence-verification', // 新增的序列验证页面路由
    name: 'SequenceVerification',
    component: SequenceVerification,
    meta: {
      description:
        'Compare generated tRNA sequences with natural tRNA sequences for verification using tRNAscan-SE tool.', // 为页面添加描述
    },
  },
  { path: '/', name: 'Help', component: Home },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

export default router
