<template>
  <el-form label-width="100px" size="mini">
    <el-form-item label="参数(JSON)">
      <el-input
        type="textarea"
        v-model="internalValue.jsonString"
        :rows="4"
        placeholder='{"key": "value"}'
      />
    </el-form-item>
  </el-form>
</template>

<script>
export default {
  name: 'DefaultForm',
  props: ['value'],
  computed: {
    internalValue: {
      get() { return this.value || { jsonString: '{}' }; },
      set(val) { this.$emit('input', val); }
    }
  }
};
</script>