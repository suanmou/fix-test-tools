<template>
  <el-form label-width="120px" size="mini">
    <el-form-item label="风险类型">
      <el-select v-model="internalValue.riskType" placeholder="选择类型">
        <el-option label="仓位检查" value="POSITION" />
        <el-option label="资金检查" value="MARGIN" />
        <el-option label="价格检查" value="PRICE" />
        <el-option label="数量检查" value="QUANTITY" />
      </el-select>
    </el-form-item>
    <el-form-item label="股票代码">
      <el-input v-model="internalValue.symbol" placeholder="如: AAPL" />
    </el-form-item>
    <el-form-item label="检查参数">
      <el-input
        type="textarea"
        v-model="internalValue.parameters"
        :rows="2"
        placeholder="JSON格式参数"
      />
    </el-form-item>
  </el-form>
</template>

<script>
export default {
  name: 'RiskCheckForm',
  props: ['value'],
  computed: {
    internalValue: {
      get() { return this.value || { riskType: 'POSITION', symbol: '', parameters: '{}' }; },
      set(val) { this.$emit('input', val); }
    }
  }
};
</script>