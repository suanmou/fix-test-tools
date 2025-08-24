<template>
  <el-form label-width="120px" size="mini">
    <el-form-item label="订单号">
      <el-input v-model="internalValue.clOrdID" placeholder="订单编号" />
    </el-form-item>
    <el-form-item label="股票代码">
      <el-input v-model="internalValue.symbol" placeholder="如: AAPL" />
    </el-form-item>
    <el-form-item label="查询类型">
      <el-select v-model="internalValue.queryType" placeholder="选择类型">
        <el-option label="按订单号查询" value="BY_ORDER" />
        <el-option label="按股票代码查询" value="BY_SYMBOL" />
        <el-option label="查询所有订单" value="ALL" />
      </el-select>
    </el-form-item>
  </el-form>
</template>

<script>
export default {
  name: 'QueryStatusForm',
  props: ['value'],
  computed: {
    internalValue: {
      get() { return this.value || { clOrdID: '', symbol: '', queryType: 'BY_ORDER' }; },
      set(val) { this.$emit('input', val); }
    }
  }
};
</script>