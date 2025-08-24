<template>
  <el-form label-width="120px" size="mini">
    <el-form-item label="消息类型">
      <el-input v-model="internalValue.messageType" placeholder="8" />
    </el-form-item>
    <el-form-item label="执行类型">
      <el-select v-model="internalValue.execType" placeholder="选择类型">
        <el-option label="新订单确认" value="0" />
        <el-option label="部分成交" value="1" />
        <el-option label="全部成交" value="2" />
        <el-option label="已取消" value="4" />
        <el-option label="已替换" value="5" />
        <el-option label="待成交" value="6" />
      </el-select>
    </el-form-item>
    <el-form-item label="订单状态">
      <el-select v-model="internalValue.ordStatus" placeholder="选择状态">
        <el-option label="新订单" value="0" />
        <el-option label="部分成交" value="1" />
        <el-option label="全部成交" value="2" />
        <el-option label="已取消" value="4" />
        <el-option label="已替换" value="5" />
      </el-select>
    </el-form-item>
    <el-form-item label="成交数量">
      <el-input-number v-model="internalValue.lastQty" :min="0" />
    </el-form-item>
    <el-form-item label="成交价格">
      <el-input-number v-model="internalValue.lastPx" :min="0" :precision="4" />
    </el-form-item>
    <el-form-item label="剩余数量">
      <el-input-number v-model="internalValue.leavesQty" :min="0" />
    </el-form-item>
    <el-form-item label="累计数量">
      <el-input-number v-model="internalValue.cumQty" :min="0" />
    </el-form-item>
  </el-form>
</template>

<script>
export default {
  name: 'ExecutionReportForm',
  props: ['value'],
  computed: {
    internalValue: {
      get() { 
        return this.value || { 
          messageType: '8', 
          execType: '0', 
          ordStatus: '0', 
          lastQty: 0, 
          lastPx: 0, 
          leavesQty: 0, 
          cumQty: 0 
        }; 
      },
      set(val) { this.$emit('input', val); }
    }
  }
};
</script>