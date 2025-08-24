import Vue from 'vue'
import Router from 'vue-router'
import ScenarioManager from '@/views/ScenarioManager'
import ScenarioEditor from '@/views/ScenarioEditor'

Vue.use(Router)

export default new Router({
  routes: [
    {
      path: '/',
      name: 'ScenarioManager',
      component: ScenarioManager
    },
    {
      path: '/editor',
      name: 'ScenarioEditor',
      component: ScenarioEditor
    },
    {
      path: '/editor/:id',
      name: 'ScenarioEditorWithId',
      component: ScenarioEditor
    }
  ]
})