import { createRouter, createWebHistory } from 'vue-router'
import Home from '../pages/HomePage/HomePage.vue'
import CodonGenerator from '../pages/CondonGenerator/CondonGenerator.vue'
import ResultAnalysis from '../pages/ResultAnalysis/ResultAnalysis.vue'

const routes = [
  { path: '/', name: 'Home', component: Home },
  { path: '/generator', name: 'CodonGenerator', component: CodonGenerator },
  { path: '/analysis', name: 'ResultAnalysis', component: ResultAnalysis },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

export default router
