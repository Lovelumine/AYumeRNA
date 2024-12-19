import { createRouter, createWebHistory } from 'vue-router'
import Home from '../pages/HomePage/HomePage.vue'
import CodonGenerator from '../pages/CondonGenerator/CondonGenerator.vue'
// import TReXScore from '../pages/TReXScore/TReXScore.vue'
import VisualizationAnalysis from '../pages/ResultAnalysis/VisualizationAnalysis.vue'
import SecondaryStructure from '../pages/ResultAnalysis/SecondaryStructure/SecondaryStructure.vue'
import TertiaryStructure from '../pages/ResultAnalysis/TertiaryStructure/TertiaryStructure.vue'
// import SequenceVerification from '../pages/SequenceVerification/SequenceVerification.vue'
import TRNAEvaluator from '../pages/TRNAEvaluator/TRNAEvaluator.vue'
import AminoacylationEvaluation from '../pages/TRNAEvaluator/AminoacylationEvaluation/AminoacylationEvaluation.vue'
import StructureFoldingEvaluation from '../pages/TRNAEvaluator/StructureFoldingEvaluation/StructureFoldingEvaluation.vue'
import IdentityElementsEvaluation from '../pages/TRNAEvaluator/IdentityElementsEvaluation/IdentityElementsEvaluation.vue'
import AboutPage from '../pages/AboutPage/AboutPage.vue'


const routes = [

  { path: '/', name: 'Home', component: Home },
  { path: '/CodonGenerator', name: 'Sequence Generator', component: CodonGenerator },
  {
    path: '/trna-evaluator',
    name: 'TRNAEvaluator',
    component: TRNAEvaluator,
    children: [

      {
        path: 'structure-folding',
        name: 'StructureFolding Evaluation',
        component: StructureFoldingEvaluation,
      },
      {
        path: 'identity-elements',
        name: 'IdentityElements Evaluation',
        component: IdentityElementsEvaluation,
      },
      {
        path: 'aminoacylation',
        name: 'The affinity between aa-tRNAs and EF-Tu',
        component: AminoacylationEvaluation,
      },
    ],
  },
  // {
  //   path: '/trex-score',
  //   name: 'tRNACompatibility Evaluator',
  //   component: TReXScore,
  // },
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
  // {
  //   path: '/sequence-verification',
  //   name: 'SequenceVerification',
  //   component: SequenceVerification,
  //   meta: {
  //     description:
  //       'Compare generated tRNA sequences with natural tRNA sequences for verification using tRNAscan-SE tool.',
  //   },
  // },

  { path: '/about', name: 'About', component: AboutPage },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

export default router
