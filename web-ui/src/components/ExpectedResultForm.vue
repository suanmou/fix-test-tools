<template>
  <div>
    <el-form label-width="100px" size="mini">
      <el-form-item label="结果类型">
        <el-select v-model="resultType" placeholder="选择结果类型" size="mini" @change="updateTemplate">
          <el-option label="通用结果" value="generic" />
          <el-option label="订单结果" value="order" />
          <el-option label="执行报告" value="execution" />
          <el-option label="登录结果" value="login" />
          <el-option label="心跳响应" value="heartbeat" />
          <el-option label="市场数据" value="marketdata" />
          <el-option label="风险检查" value="risk" />
        </el-select>
      </el-form-item>
    </el-form>

    <!-- 动态表单区域 -->
    <div v-if="resultType === 'generic'">
      <EnhancedJsonEditor v-model="internalValue" />
    </div>

    <div v-else-if="resultType === 'order'">
      <el-form label-width="120px" size="mini">
        <el-form-item label="订单状态">
          <el-select v-model="internalValue.ordStatus" size="mini">
            <el-option label="新订单" value="0" />
            <el-option label="部分成交" value="1" />
            <el-option label="全部成交" value="2" />
            <el-option label="已取消" value="4" />
            <el-option label="已替换" value="5" />
          </el-select>
        </el-form-item>
        <el-form-item label="执行类型">
          <el-select v-model="internalValue.execType" size="mini">
            <el-option label="新订单确认" value="0" />
            <el-option label="部分成交" value="1" />
            <el-option label="全部成交" value="2" />
            <el-option label="已取消" value="4" />
          </el-select>
        </el-form-item>
        <el-form-item label="消息">
          <el-input v-model="internalValue.message" placeholder="操作结果消息" size="mini" />
        </el-form-item>
      </el-form>
    </div>

    <div v-else-if="resultType === 'execution'">
      <el-form label-width="120px" size="mini">
        <el-form-item label="消息类型">
          <el-input v-model="internalValue.messageType" placeholder="8" size="mini" />
        </el-form-item>
        <el-form-item label="执行类型">
          <el-select v-model="internalValue.execType" size="mini">
            <el-option label="新订单确认" value="0" />
            <el-option label="部分成交" value="1" />
            <el-option label="全部成交" value="2" />
            <el-option label="已取消" value="4" />
            <el-option label="已替换" value="5" />
          </el-select>
        </el-form-item>
        <el-form-item label="订单状态">
          <el-select v-model="internalValue.ordStatus" size="mini">
            <el-option label="新订单" value="0" />
            <el-option label="部分成交" value="1" />
            <el-option label="全部成交" value="2" />
            <el-option label="已取消" value="4" />
            <el-option label="已替换" value="5" />
          </el-select>
        </el-form-item>
        <el-form-item label="成交数量">
          <el-input-number v-model="internalValue.lastQty" :min="0" size="mini" />
        </el-form-item>
        <el-form-item label="成交价格">
          <el-input-number v-model="internalValue.lastPx" :min="0" :precision="4" size="mini" />
        </el-form-item>
      </el-form>
    </div>

    <div v-else-if="resultType === 'login'">
      <el-form label-width="120px" size="mini">
        <el-form-item label="登录成功">
          <el-switch v-model="internalValue.success" size="mini" />
        </el-form-item>
        <el-form-item label="用户名">
          <el-input v-model="internalValue.username" placeholder="用户名" size="mini" />
        </el-form-item>
        <el-form-item label="会话ID">
          <el-input v-model="internalValue.sessionId" placeholder="会话标识" size="mini" />
        </el-form-item>
        <el-form-item label="消息">
          <el-input v-model="internalValue.message" placeholder="登录结果消息" size="mini" />
        </el-form-item>
      </el-form>
    </div>

    <div v-else-if="resultType === 'heartbeat'">
      <el-form label-width="120px" size="mini">
        <el-form-item label="心跳间隔">
          <el-input-number v-model="internalValue.heartBtInt" :min="1" :max="300" size="mini" />
        </el-form-item>
        <el-form-item label="测试请求ID">
          <el-input v-model="internalValue.testReqID" placeholder="测试请求ID" size="mini" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="internalValue.status" size="mini">
            <el-option label="正常" value="OK" />
            <el-option label="超时" value="TIMEOUT" />
            <el-option label="错误" value="ERROR" />
          </el-select>
        </el-form-item>
      </el-form>
    </div>

    <div v-else-if="resultType === 'marketdata'">
      <el-form label-width="120px" size="mini">
        <el-form-item label="数据状态">
          <el-select v-model="internalValue.status" size="mini">
            <el-option label="成功" value="SUCCESS" />
            <el-option label="失败" value="FAILURE" />
            <el-option label="超时" value="TIMEOUT" />
          </el-select>
        </el-form-item>
        <el-form-item label="股票代码">
          <el-input v-model="internalValue.symbol" placeholder="股票代码" size="mini" />
        </el-form-item>
        <el-form-item label="最新价">
          <el-input-number v-model="internalValue.lastPrice" :min="0" :precision="4" size="mini" />
        </el-form-item>
        <el-form-item label="成交量">
          <el-input-number v-model="internalValue.volume" :min="0" size="mini" />
        </el-form-item>
      </el-form>
    </div>

    <div v-else-if="resultType === 'risk'">
      <el-form label-width="120px" size="mini">
        <el-form-item label="检查结果">
          <el-select v-model="internalValue.riskCheck" size="mini">
            <el-option label="通过" value="PASS" />
            <el-option label="拒绝" value="REJECT" />
            <el-option label="警告" value="WARNING" />
          </el-select>
        </el-form-item>
        <el-form-item label="股票代码">
          <el-input v-model="internalValue.symbol" placeholder="股票代码" size="mini" />
        </el-form-item>
        <el-form-item label="消息">
          <el-input v-model="internalValue.message" placeholder="检查结果消息" size="mini" />
        </el-form-item>
      </el-form>
    </div>
  </div>
</template>

<script>
export default {
  name: 'ExpectedResultForm',
  props: ['value'],
  components: {
    EnhancedJsonEditor: () => import('./EnhancedJsonEditor.vue')
  },
  data() {
    return {
      resultType: 'generic'
    };
  },
  computed: {
    internalValue: {
      get() {
        return this.value || {};
      },
      set(val) {
        this.$emit('input', val);
      }
    }
  },
  methods: {
    updateTemplate() {
      const templates = {
        generic: {},
        order: { ordStatus: '0', execType: '0', message: '订单已提交' },
        execution: { messageType: '8', execType: '0', ordStatus: '0', lastQty: 0, lastPx: 0 },
        login: { success: true, username: '', sessionId: '', message: '登录成功' },
        heartbeat: { heartBtInt: 30, testReqID: '', status: 'OK' },
        marketdata: { status: 'SUCCESS', symbol: '', lastPrice: 0, volume: 0 },
        risk: { riskCheck: 'PASS', symbol: '', message: '风险检查通过' }
      };
      
      this.internalValue = templates[this.resultType] || {};
    }
  }
};
</script>