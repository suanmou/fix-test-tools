<template>
  <div>
    <el-row :gutter="10">
      <el-col :span="16">
        <el-form label-width="100px" size="mini">
          <el-form-item label="编辑模式">
            <el-radio-group v-model="editMode" size="mini">
              <el-radio-button label="ui">UI模式</el-radio-button>
              <el-radio-button label="json">JSON模式</el-radio-button>
            </el-radio-group>
          </el-form-item>
        </el-form>
      </el-col>
      <el-col :span="8">
        <el-button-group size="mini">
          <el-button @click="formatJson" icon="el-icon-brush" size="mini">格式化</el-button>
          <el-button @click="validateJson" icon="el-icon-check" size="mini">验证</el-button>
          <el-button @click="clearAll" icon="el-icon-delete" size="mini">清空</el-button>
        </el-button-group>
      </el-col>
    </el-row>

    <!-- UI模式 -->
    <div v-if="editMode === 'ui'">
      <el-row :gutter="10" v-for="(item, index) in keyValuePairs" :key="index">
        <el-col :span="10">
          <el-input v-model="item.key" placeholder="键" size="mini" @change="updateJson" />
        </el-col>
        <el-col :span="10">
          <el-input v-model="item.value" placeholder="值" size="mini" @change="updateJson" />
        </el-col>
        <el-col :span="4">
          <el-button @click="removePair(index)" icon="el-icon-delete" size="mini" circle />
        </el-col>
      </el-row>
      <el-button @click="addPair" icon="el-icon-plus" size="mini" style="margin-top: 10px">添加键值对</el-button>
    </div>

    <!-- JSON模式 -->
    <div v-else>
      <el-input
        type="textarea"
        v-model="jsonString"
        :rows="6"
        placeholder='{"success": true, "message": "操作成功"}'
        @blur="parseJsonString"
      />
    </div>

    <!-- JSON预览 -->
    <el-divider>JSON预览</el-divider>
    <pre class="json-preview-small">{{ formattedJson }}</pre>
  </div>
</template>

<script>
export default {
  name: 'EnhancedJsonEditor',
  props: ['value'],
  data() {
    return {
      editMode: 'ui',
      jsonString: '{}',
      keyValuePairs: [],
      internalValue: {}
    };
  },
  computed: {
    formattedJson() {
      return JSON.stringify(this.internalValue, null, 2);
    }
  },
  watch: {
    value: {
      immediate: true,
      handler(newVal) {
        this.internalValue = newVal || {};
        this.updateKeyValuePairs();
        this.jsonString = JSON.stringify(this.internalValue, null, 2);
      }
    }
  },
  methods: {
    updateKeyValuePairs() {
      this.keyValuePairs = Object.entries(this.internalValue).map(([key, value]) => ({
        key,
        value: String(value)
      }));
    },
    updateJson() {
      const newObj = {};
      this.keyValuePairs.forEach(item => {
        if (item.key.trim()) {
          newObj[item.key.trim()] = this.parseValue(item.value);
        }
      });
      this.internalValue = newObj;
      this.jsonString = JSON.stringify(newObj, null, 2);
      this.$emit('input', newObj);
    },
    parseValue(value) {
      if (value === 'true') return true;
      if (value === 'false') return false;
      if (!isNaN(value) && value !== '') return Number(value);
      return value;
    },
    parseJsonString() {
      try {
        const parsed = JSON.parse(this.jsonString);
        this.internalValue = parsed;
        this.updateKeyValuePairs();
        this.$emit('input', parsed);
      } catch (error) {
        this.$message.error('JSON格式错误: ' + error.message);
      }
    },
    addPair() {
      this.keyValuePairs.push({ key: '', value: '' });
    },
    removePair(index) {
      this.keyValuePairs.splice(index, 1);
      this.updateJson();
    },
    formatJson() {
      this.jsonString = JSON.stringify(this.internalValue, null, 2);
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
      this.internalValue = {};
      this.keyValuePairs = [];
      this.jsonString = '{}';
      this.$emit('input', {});
    }
  }
};
</script>

<style scoped>
.json-preview-small {
  background-color: #f5f5f5;
  padding: 10px;
  border-radius: 4px;
  font-size: 12px;
  max-height: 200px;
  overflow-y: auto;
  white-space: pre-wrap;
}
</style>