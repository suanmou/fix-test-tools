<template>
  <el-form label-width="120px" size="mini">
    <el-form-item label="原订单号" required>
      <el-input v-model="internalValue.origClOrdID" placeholder="原订单编号" />
    </el-form-item>
    <el-form-item label="新订单号" required>
      <el-input v-model="internalValue.clOrdID" placeholder="取消订单编号" />
    </el-form-item>
    <el-form-item label="股票代码" required>
      <el-input v-model="internalValue.symbol" placeholder="如: AAPL" />
    </el-form-item>
    <el-form-item label="取消数量">
      <el-input-number v-model="internalValue.orderQty" :min="1" />
    </el-form-item>
    <el-form-item label="取消原因">
      <el-select v-model="internalValue.cancelReason" placeholder="选择原因">
        <el-option label="用户取消" value="U" />
        <el-option label="系统取消" value="S" />
        <el-option label="超时取消" value="T" />
      </el-select>
    </el-form-item>
  </el-form>
</template>

<script>
export default {
  name: 'CancelOrderForm',
  props: ['value'],
  computed: {
    internalValue: {
      get() { 
        return this.value || { 
          origClOrdID: '', 
          clOrdID: '', 
          symbol: '', 
          orderQty: 0, 
          cancelReason: 'U' 
        }; 
      },
      set(val) { this.$emit('input', val); }
    }
  }
};
</script>