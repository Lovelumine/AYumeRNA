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
import HelpPage from '../pages/HelpPage/HelpPage.vue'

const routes = [

  { path: '/', name: 'Home', component: Home },
  { path: '/CodonGenerator', name: 'Sequence Generator', component: CodonGenerator },
  {
    path: '/trna-evaluator',
    name: 'tRNA Evaluator',
    component: TRNAEvaluator,
    children: [

      {
        path: 'structure-folding',
        name: 'Structure Folding Evaluation',
        component: StructureFoldingEvaluation,
      },
      {
        path: 'identity-elements',
        name: 'Identity Elements Evaluation',
        component: IdentityElementsEvaluation,
      },
      {
        path: 'aminoacylation',
        name: 'The affinity between aa-tRNAs and release factor',
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
    name: 'Visualization Analysis',
    component: VisualizationAnalysis,
    children: [
      {
        path: 'secondary-structure',
        name: 'Secondary Structure',
        component: SecondaryStructure,
      },
      {
        path: 'tertiary-structure',
        name: 'Tertiary Structure',
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
  { path: '/help', name: 'Help', component: HelpPage },
  { path: '/about', name: 'About', component: AboutPage },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

export default router
