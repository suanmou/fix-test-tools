import Vue from 'vue'
import App from './App'
import router from './router'
import store from './store'
import ElementUI from 'element-ui'
import 'element-ui/lib/theme-chalk/index.css'
import axios from 'axios'

Vue.use(ElementUI)
Vue.config.productionTip = false

// 配置axios
axios.defaults.baseURL = 'http://localhost:8080/api'
Vue.prototype.$http = axios

new Vue({
  el: '#app',
  router,
  store,
  components: { App },
  template: '<App/>'
})