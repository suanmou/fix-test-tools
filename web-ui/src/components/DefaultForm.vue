<template>
  <div>
    <el-form label-width="100px" size="mini">
      <el-form-item label="编辑模式">
        <el-radio-group v-model="editMode" size="mini">
          <el-radio-button label="ui">UI模式</el-radio-button>
          <el-radio-button label="json">JSON模式</el-radio-button>
          <el-radio-button label="template">模板模式</el-radio-button>
        </el-radio-group>
      </el-form-item>
    </el-form>

    <!-- UI模式 - 键值对编辑 -->
    <div v-if="editMode === 'ui'">
      <el-row :gutter="10" v-for="(item, index) in keyValuePairs" :key="index" style="margin-bottom: 5px">
        <el-col :span="9">
          <el-input v-model="item.key" placeholder="键" size="mini" @input="updateFromUI" />
        </el-col>
        <el-col :span="9">
          <el-input v-model="item.value" placeholder="值" size="mini" @input="updateFromUI" />
        </el-col>
        <el-col :span="6">
          <el-button @click="removePair(index)" icon="el-icon-delete" size="mini" circle />
        </el-col>
      </el-row>
      <el-button @click="addPair" icon="el-icon-plus" size="mini" style="margin-top: 10px">添加键值对</el-button>
    </div>

    <!-- JSON模式 - 直接编辑 -->
    <div v-else-if="editMode === 'json'">
      <el-input
        type="textarea"
        v-model="jsonString"
        :rows="6"
        placeholder='{"success": true, "message": "操作成功"}'
        @blur="parseJson"
      />
    </div>

    <!-- 模板模式 - 快速选择 -->
    <div v-else>
      <el-select v-model="selectedTemplate" placeholder="选择预设模板" size="mini" style="width: 100%; margin-bottom: 10px">
        <el-option
          v-for="template in jsonTemplates"
          :key="template.name"
          :label="template.name"
          :value="template.value"
        />
      </el-select>
      <el-input
        type="textarea"
        v-model="templateJson"
        :rows="4"
        readonly
      />
    </div>

    <!-- 操作按钮 -->
    <el-button-group size="mini" style="margin-top: 10px">
      <el-button @click="formatJson" icon="el-icon-brush">格式化</el-button>
      <el-button @click="validateJson" icon="el-icon-check">验证</el-button>
      <el-button @click="clearAll" icon="el-icon-delete">清空</el-button>
    </el-button-group>

    <!-- 实时预览 -->
    <el-divider>JSON预览</el-divider>
    <pre class="json-preview-small">{{ formattedJson }}</pre>
  </div>
</template>

<script>
export default {
  name: 'DefaultForm',
  props: ['value'],
  data() {
    return {
      editMode: 'ui',
      jsonString: '{}',
      keyValuePairs: [],
      selectedTemplate: null,
      templateJson: '',
      jsonTemplates: [
        { name: '成功响应', value: { success: true, message: '操作成功' } },
        { name: '失败响应', value: { success: false, message: '操作失败', error: '错误详情' } },
        { name: '订单状态', value: { ordStatus: '0', message: '订单已提交' } },
        { name: '执行报告', value: { execType: '0', ordStatus: '0', lastQty: 0, lastPx: 0 } },
        { name: '心跳响应', value: { heartBtInt: 30, testReqID: '' } },
        { name: '登录成功', value: { loggedIn: true, username: '', sessionId: '' } },
        { name: '风险检查', value: { riskCheck: 'PASS', message: '风险检查通过' } }
      ]
    };
  },
  computed: {
    formattedJson() {
      return JSON.stringify(this.internalValue, null, 2);
    },
    internalValue() {
      return this.value || {};
    }
  },
  watch: {
    value: {
      immediate: true,
      handler(newVal) {
        const val = newVal || {};
        this.jsonString = JSON.stringify(val, null, 2);
        this.updateKeyValuePairs(val);
      }
    },
    selectedTemplate(newVal) {
      if (newVal) {
        this.$emit('input', newVal);
        this.templateJson = JSON.stringify(newVal, null, 2);
      }
    }
  },
  methods: {
    updateKeyValuePairs(obj) {
      this.keyValuePairs = Object.entries(obj).map(([key, value]) => ({
        key,
        value: String(value)
      }));
    },
    updateFromUI() {
      const newObj = {};
      this.keyValuePairs.forEach(item => {
        if (item.key.trim()) {
          newObj[item.key.trim()] = this.parseValue(item.value);
        }
      });
      this.jsonString = JSON.stringify(newObj, null, 2);
      this.$emit('input', newObj);
    },
    parseValue(value) {
      if (value === 'true') return true;
      if (value === 'false') return false;
      if (!isNaN(value) && value !== '') return Number(value);
      return value;
    },
    parseJson() {
      try {
        const parsed = JSON.parse(this.jsonString);
        this.$emit('input', parsed);
        this.updateKeyValuePairs(parsed);
      } catch (error) {
        this.$message.error('JSON格式错误: ' + error.message);
      }
    },
    addPair() {
      this.keyValuePairs.push({ key: '', value: '' });
    },
    removePair(index) {
      this.keyValuePairs.splice(index, 1);
      this.updateFromUI();
    },
    formatJson() {
      try {
        const parsed = JSON.parse(this.jsonString);
        this.jsonString = JSON.stringify(parsed, null, 2);
      } catch (error) {
        this.$message.error('JSON格式错误: ' + error.message);
      }
    },
    validateJson() {
      try {
        JSON.parse(this.jsonString);
        this.$message.success('JSON格式正确');
      } catch (error) {
        this.$message.error('JSON格式错误: ' + error.message);
      }
    },
    clearAll() {
      this.jsonString = '{}';
      this.keyValuePairs = [];
      this.selectedTemplate = null;
      this.$emit('input', {});
    }
  }
};
</script>

<style scoped>
.json-preview-small {
  background-color: #f5f5f5;
  padding: 8px;
  border-radius: 4px;
  font-size: 11px;
  max-height: 150px;
  overflow-y: auto;
  white-space: pre-wrap;
  margin-top: 5px;
}
</style>
