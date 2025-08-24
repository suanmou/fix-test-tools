<template>
  <el-form label-width="100px" size="mini">
    <el-form-item label="等待时间(秒)">
      <el-input-number v-model="internalValue.waitTime" :min="1" :max="300" />
    </el-form-item>
    <el-form-item label="用户名">
      <el-input v-model="internalValue.username" placeholder="登录用户名" />
    </el-form-item>
    <el-form-item label="密码">
      <el-input v-model="internalValue.password" type="password" placeholder="登录密码" />
    </el-form-item>
  </el-form>
</template>

<script>
export default {
  name: 'WaitLoginForm',
  props: ['value'],
  computed: {
    internalValue: {
      get() { return this.value || { waitTime: 30, username: '', password: '' }; },
      set(val) { this.$emit('input', val); }
    }
  }
};
</script>