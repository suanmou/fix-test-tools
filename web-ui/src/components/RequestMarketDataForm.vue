<template>
  <el-form label-width="120px" size="mini">
    <el-form-item label="股票代码" required>
      <el-input v-model="internalValue.symbol" placeholder="如: AAPL" />
    </el-form-item>
    <el-form-item label="数据类型">
      <el-select v-model="internalValue.dataType" placeholder="选择类型">
        <el-option label="实时行情" value="LIVE" />
        <el-option label="历史数据" value="HISTORICAL" />
        <el-option label="深度行情" value="DEPTH" />
      </el-select>
    </el-form-item>
    <el-form-item label="字段列表">
      <el-checkbox-group v-model="internalValue.fields">
        <el-checkbox label="BID">买价</el-checkbox>
        <el-checkbox label="ASK">卖价</el-checkbox>
        <el-checkbox label="LAST">最新价</el-checkbox>
        <el-checkbox label="VOLUME">成交量</el-checkbox>
      </el-checkbox-group>
    </el-form-item>
  </el-form>
</template>

<script>
export default {
  name: 'RequestMarketDataForm',
  props: ['value'],
  computed: {
    internalValue: {
      get() { return this.value || { symbol: '', dataType: 'LIVE', fields: ['BID', 'ASK', 'LAST'] }; },
      set(val) { this.$emit('input', val); }
    }
  }
};
</script>