<template>
  <el-form label-width="120px" size="mini">
    <el-form-item label="订单号" required>
      <el-input v-model="internalValue.clOrdID" placeholder="如: ORD2024001" />
    </el-form-item>
    <el-form-item label="股票代码" required>
      <el-input v-model="internalValue.symbol" placeholder="如: AAPL, TSLA" />
    </el-form-item>
    <el-form-item label="买卖方向" required>
      <el-select v-model="internalValue.side" placeholder="选择方向">
        <el-option label="买入(BUY)" value="1" />
        <el-option label="卖出(SELL)" value="2" />
        <el-option label="卖空(SELL SHORT)" value="5" />
      </el-select>
    </el-form-item>
    <el-form-item label="数量" required>
      <el-input-number v-model="internalValue.orderQty" :min="1" :max="1000000" />
    </el-form-item>
    <el-form-item label="价格">
      <el-input-number v-model="internalValue.price" :min="0" :precision="4" :step="0.01" />
    </el-form-item>
    <el-form-item label="订单类型" required>
      <el-select v-model="internalValue.ordType" placeholder="选择类型">
        <el-option label="市价(MARKET)" value="1" />
        <el-option label="限价(LIMIT)" value="2" />
        <el-option label="止损(STOP)" value="3" />
        <el-option label="止损限价(STOP LIMIT)" value="4" />
      </el-select>
    </el-form-item>
    <el-form-item label="有效时间">
      <el-select v-model="internalValue.timeInForce" placeholder="选择有效时间">
        <el-option label="当日有效(DAY)" value="0" />
        <el-option label="立即成交或取消(IOC)" value="3" />
        <el-option label="全部成交或取消(FOK)" value="4" />
        <el-option label="长期有效(GTC)" value="1" />
      </el-select>
    </el-form-item>
  </el-form>
</template>

<script>
export default {
  name: 'NewOrderForm',
  props: ['value'],
  computed: {
    internalValue: {
      get() { 
        return this.value || { 
          clOrdID: '', 
          symbol: '', 
          side: '1', 
          orderQty: 100, 
          price: 100.00, 
          ordType: '2', 
          timeInForce: '0' 
        }; 
      },
      set(val) { this.$emit('input', val); }
    }
  }
};
</script>