<template>
  <el-form label-width="100px" size="mini">
    <el-form-item label="心跳间隔(秒)">
      <el-input-number v-model="internalValue.heartBtInt" :min="1" :max="60" />
    </el-form-item>
    <el-form-item label="测试请求ID">
      <el-input v-model="internalValue.testReqID" placeholder="可选测试ID" />
    </el-form-item>
  </el-form>
</template>

<script>
export default {
  name: 'HeartbeatForm',
  props: ['value'],
  computed: {
    internalValue: {
      get() { return this.value || { heartBtInt: 30, testReqID: '' }; },
      set(val) { this.$emit('input', val); }
    }
  }
};
</script>