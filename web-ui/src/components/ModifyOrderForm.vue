<template>
  <el-form label-width="120px" size="mini">
    <el-form-item label="原订单号" required>
      <el-input v-model="internalValue.origClOrdID" placeholder="原订单编号" />
    </el-form-item>
    <el-form-item label="新订单号" required>
      <el-input v-model="internalValue.clOrdID" placeholder="修改订单编号" />
    </el-form-item>
    <el-form-item label="股票代码" required>
      <el-input v-model="internalValue.symbol" placeholder="如: AAPL" />
    </el-form-item>
    <el-form-item label="新数量">
      <el-input-number v-model="internalValue.orderQty" :min="1" />
    </el-form-item>
    <el-form-item label="新价格">
      <el-input-number v-model="internalValue.price" :min="0" :precision="4" :step="0.01" />
    </el-form-item>
    <el-form-item label="修改类型">
      <el-select v-model="internalValue.modifyType" placeholder="选择类型">
        <el-option label="数量修改" value="QTY" />
        <el-option label="价格修改" value="PRICE" />
        <el-option label="全部修改" value="ALL" />
      </el-select>
    </el-form-item>
  </el-form>
</template>

<script>
export default {
  name: 'ModifyOrderForm',
  props: ['value'],
  computed: {
    internalValue: {
      get() { 
        return this.value || { 
          origClOrdID: '', 
          clOrdID: '', 
          symbol: '', 
          orderQty: 100, 
          price: 100.00, 
          modifyType: 'ALL' 
        }; 
      },
      set(val) { this.$emit('input', val); }
    }
  }
};
</script>