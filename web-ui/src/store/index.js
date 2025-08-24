import Vue from 'vue'
import Vuex from 'vuex'

Vue.use(Vuex)

export default new Vuex.Store({
  state: {
    scenarios: []
  },
  mutations: {
    SET_SCENARIOS(state, scenarios) {
      state.scenarios = scenarios
    },
    ADD_SCENARIO(state, scenario) {
      state.scenarios.push(scenario)
    },
    UPDATE_SCENARIO(state, updatedScenario) {
      const index = state.scenarios.findIndex(s => s.id === updatedScenario.id)
      if (index !== -1) {
        state.scenarios.splice(index, 1, updatedScenario)
      }
    },
    DELETE_SCENARIO(state, id) {
      const index = state.scenarios.findIndex(s => s.id === id)
      if (index !== -1) {
        state.scenarios.splice(index, 1)
      }
    }
  },
  actions: {
    async loadScenarios({ commit }) {
      try {
        const response = await Vue.prototype.$http.get('/scenarios')
        commit('SET_SCENARIOS', response.data)
      } catch (error) {
        console.error('加载场景失败:', error)
      }
    },
    
    async saveScenario({ commit }, scenario) {
      try {
        const response = await Vue.prototype.$http.post('/scenarios', scenario)
        commit('ADD_SCENARIO', response.data)
        return response.data
      } catch (error) {
        console.error('保存场景失败:', error)
        throw error
      }
    },
    
    async updateScenario({ commit }, scenario) {
      try {
        const response = await Vue.prototype.$http.put(`/scenarios/${scenario.id}`, scenario)
        commit('UPDATE_SCENARIO', response.data)
        return response.data
      } catch (error) {
        console.error('更新场景失败:', error)
        throw error
      }
    },
    
    async deleteScenario({ commit }, id) {
      try {
        await Vue.prototype.$http.delete(`/scenarios/${id}`)
        commit('DELETE_SCENARIO', id)
      } catch (error) {
        console.error('删除场景失败:', error)
        throw error
      }
    }
  }
})