<template>
  <div class="scenario-editor">
    <el-row :gutter="20">
      <el-col :span="16">
        <el-card>
          <div slot="header">
            <span>{{ isEdit ? '编辑测试场景' : '创建测试场景' }}</span>
            <el-button
              type="primary"
              style="float: right"
              @click="saveScenario"
            >
              保存
            </el-button>
            <el-button
              style="float: right; margin-right: 10px"
              @click="$router.push('/')"
            >
              返回
            </el-button>
          </div>

          <el-form :model="scenario" label-width="120px">
            <el-form-item label="场景ID">
              <el-input v-model="scenario.id" :disabled="isEdit"></el-input>
            </el-form-item>

            <el-form-item label="场景名称">
              <el-input v-model="scenario.name"></el-input>
            </el-form-item>

            <el-form-item label="描述">
              <el-input
                type="textarea"
                v-model="scenario.description"
                :rows="3"
              >
              </el-input>
            </el-form-item>

            <!-- 元数据可视化编辑器 -->
            <el-form-item label="元数据">
              <el-card shadow="never" class="json-editor-card">
                <div slot="header" class="card-header">
                  <span>元数据配置</span>
                  <el-button type="text" @click="addMetadataItem">添加字段</el-button>
                </div>
                <div v-for="(value, key, index) in scenario.metadata" :key="index" class="json-item">
                  <el-input v-model="metadataKeys[index]" placeholder="键名" style="width: 40%; margin-right: 10px;" />
                  <el-input v-model="metadataValues[index]" placeholder="值" style="width: 40%; margin-right: 10px;" />
                  <el-button type="danger" icon="el-icon-delete" circle @click="removeMetadataItem(key)" />
                </div>
                <div v-if="Object.keys(scenario.metadata).length === 0" class="empty-tip">
                  <span>暂无元数据，点击"添加字段"按钮添加</span>
                </div>
              </el-card>
            </el-form-item>

            <!-- 配置可视化编辑器 -->
            <el-form-item label="配置">
              <el-card shadow="never" class="json-editor-card">
                <div slot="header" class="card-header">
                  <span>系统配置</span>
                  <el-button type="text" @click="addConfigItem">添加配置</el-button>
                </div>
                <div v-for="(value, key, index) in scenario.configuration" :key="index" class="json-item">
                  <el-input v-model="configKeys[index]" placeholder="配置项" style="width: 40%; margin-right: 10px;" />
                  <el-input v-model="configValues[index]" placeholder="值" style="width: 40%; margin-right: 10px;" />
                  <el-button type="danger" icon="el-icon-delete" circle @click="removeConfigItem(key)" />
                </div>
                <div v-if="Object.keys(scenario.configuration).length === 0" class="empty-tip">
                  <span>暂无配置，点击"添加配置"按钮添加</span>
                </div>
              </el-card>
            </el-form-item>

            <el-divider>测试步骤</el-divider>

            <!-- 测试步骤的动态表单 -->
            <div v-for="(step, index) in scenario.sequence" :key="index" class="step-card">
              <el-card shadow="hover" class="step-item">
                <div slot="header" class="step-header">
                  <span>步骤 {{ index + 1 }}: {{ getActionLabel(step.action) }}</span>
                  <el-button type="danger" icon="el-icon-delete" @click="removeStep(index)" />
                </div>

                <el-form label-width="100px" size="small">
                  <el-form-item label="操作类型">
                    <el-select v-model="step.action" placeholder="选择操作" style="width: 100%">
                      <el-option
                        v-for="action in availableActions"
                        :key="action.value"
                        :label="action.label"
                        :value="action.value"
                      />
                    </el-select>
                  </el-form-item>

                  <el-form-item label="超时时间(毫秒)">
                    <el-input-number
                      v-model="step.timeout"
                      :min="1000"
                      :max="60000"
                      :step="1000"
                      style="width: 100%"
                    />
                  </el-form-item>

                  <!-- 动态参数表单 -->
                  <el-form-item label="参数">
                    <component
                      :is="getParameterFormComponent(step.action)"
                      v-model="step.parameters"
                    />
                  </el-form-item>

                  <!-- 动态预期结果表单 -->
                  <el-form-item label="预期结果">
                    <component
                      :is="getExpectedFormComponent(step.action)"
                      v-model="step.expected"
                    />
                  </el-form-item>
                </el-form>
              </el-card>
            </div>

            <el-button type="primary" icon="el-icon-plus" @click="addStep" style="margin-top: 10px">
              添加步骤
            </el-button>
          </el-form>
        </el-card>
      </el-col>

      <el-col :span="8">
        <el-card>
          <div slot="header">
            <span>JSON预览</span>
            <el-button type="text" style="float: right" @click="validateJson">
              验证JSON
            </el-button>
          </div>
          <pre class="json-preview">{{ jsonPreview }}</pre>
        </el-card>
      </el-col>
    </el-row>

    <!-- 参数表单组件 -->
    <script type="text/x-template" id="wait-login-form">
      <el-form label-width="80px" size="mini">
        <el-form-item label="等待时间">
          <el-input-number v-model="value.waitTime" :min="1" :max="30" />
        </el-form-item>
      </el-form>
    </script>

    <script type="text/x-template" id="new-order-form">
      <el-form label-width="100px" size="mini">
        <el-form-item label="订单号">
          <el-input v-model="value.clOrdID" placeholder="订单编号" />
        </el-form-item>
        <el-form-item label="股票代码">
          <el-input v-model="value.symbol" placeholder="如: AAPL" />
        </el-form-item>
        <el-form-item label="买卖方向">
          <el-select v-model="value.side" placeholder="选择方向">
            <el-option label="买入" value="1" />
            <el-option label="卖出" value="2" />
          </el-select>
        </el-form-item>
        <el-form-item label="数量">
          <el-input-number v-model="value.orderQty" :min="1" />
        </el-form-item>
        <el-form-item label="价格">
          <el-input-number v-model="value.price" :min="0" :precision="2" />
        </el-form-item>
        <el-form-item label="订单类型">
          <el-select v-model="value.ordType" placeholder="选择类型">
            <el-option label="市价" value="1" />
            <el-option label="限价" value="2" />
          </el-select>
        </el-form-item>
      </el-form>
    </script>

    <script type="text/x-template" id="cancel-order-form">
      <el-form label-width="100px" size="mini">
        <el-form-item label="原订单号">
          <el-input v-model="value.origClOrdID" placeholder="原订单编号" />
        </el-form-item>
        <el-form-item label="新订单号">
          <el-input v-model="value.clOrdID" placeholder="取消订单编号" />
        </el-form-item>
        <el-form-item label="股票代码">
          <el-input v-model="value.symbol" placeholder="如: AAPL" />
        </el-form-item>
        <el-form-item label="数量">
          <el-input-number v-model="value.orderQty" :min="1" />
        </el-form-item>
        <el-form-item label="买卖方向">
          <el-select v-model="value.side" placeholder="选择方向">
            <el-option label="买入" value="1" />
            <el-option label="卖出" value="2" />
          </el-select>
        </el-form-item>
      </el-form>
    </script>

    <script type="text/x-template" id="default-form">
      <el-form label-width="80px" size="mini">
        <el-form-item label="参数">
          <el-input
            type="textarea"
            v-model="value.jsonString"
            :rows="3"
            placeholder="JSON格式参数"
          />
        </el-form-item>
      </el-form>
    </script>

    <script type="text/x-template" id="execution-report-form">
      <el-form label-width="100px" size="mini">
        <el-form-item label="消息类型">
          <el-input v-model="value.messageType" placeholder="8" />
        </el-form-item>
        <el-form-item label="执行类型">
          <el-select v-model="value.execType" placeholder="选择类型">
            <el-option label="新订单确认" value="0" />
            <el-option label="部分成交" value="1" />
            <el-option label="全部成交" value="2" />
            <el-option label="已取消" value="4" />
          </el-select>
        </el-form-item>
        <el-form-item label="订单状态">
          <el-select v-model="value.ordStatus" placeholder="选择状态">
            <el-option label="新订单" value="0" />
            <el-option label="部分成交" value="1" />
            <el-option label="全部成交" value="2" />
            <el-option label="已取消" value="4" />
          </el-select>
        </el-form-item>
      </el-form>
    </script>
  </div>
</template>

<script>
export default {
  name: 'ScenarioEditor',
  data() {
    return {
      scenario: {
        id: '',
        name: '',
        description: '',
        metadata: {},
        configuration: {},
        sequence: [],
      },
      metadataKeys: [],
      metadataValues: [],
      configKeys: [],
      configValues: [],
      availableActions: [
        { value: 'WAIT_LOGIN', label: '等待登录' },
        { value: 'SEND_HEARTBEAT', label: '发送心跳' },
        { value: 'SEND_TEST_REQUEST', label: '发送测试请求' },
        { value: 'SEND_NEW_ORDER', label: '发送新订单' },
        { value: 'CANCEL_ORDER', label: '取消订单' },
        { value: 'MODIFY_ORDER', label: '修改订单' },
        { value: 'QUERY_ORDER_STATUS', label: '查询订单状态' },
        { value: 'WAIT_EXECUTION_REPORT', label: '等待执行报告' },
        { value: 'WAIT_ORDER_CANCEL_RESPONSE', label: '等待取消响应' },
        { value: 'WAIT_ORDER_MODIFY_RESPONSE', label: '等待修改响应' },
      ],
    };
  },
  computed: {
    isEdit() {
      return !!this.$route.params.id;
    },
    jsonPreview() {
      const scenario = {
        id: this.scenario.id,
        name: this.scenario.name,
        description: this.scenario.description,
        metadata: this.scenario.metadata,
        configuration: this.scenario.configuration,
        sequence: this.scenario.sequence.map((step) => ({
          action: step.action,
          parameters: step.parameters || {},
          timeout: step.timeout,
          expected: step.expected || null,
        })),
      };
      return JSON.stringify(scenario, null, 2);
    },
  },
  watch: {
    metadataKeys: {
      handler() {
        this.updateMetadata();
      },
      deep: true,
    },
    metadataValues: {
      handler() {
        this.updateMetadata();
      },
      deep: true,
    },
    configKeys: {
      handler() {
        this.updateConfiguration();
      },
      deep: true,
    },
    configValues: {
      handler() {
        this.updateConfiguration();
      },
      deep: true,
    },
  },
  created() {
    if (this.isEdit) {
      this.loadScenario();
    } else {
      this.scenario.id = 'scenario_' + Date.now();
    }
  },
  methods: {
    loadScenario() {
      const id = this.$route.params.id;
      this.$http.get(`/scenarios/${id}`).then((response) => {
        this.scenario = response.data;
        
        // 转换元数据
        this.metadataKeys = Object.keys(this.scenario.metadata || {});
        this.metadataValues = this.metadataKeys.map(key => this.scenario.metadata[key]);
        
        // 转换配置
        this.configKeys = Object.keys(this.scenario.configuration || {});
        this.configValues = this.configKeys.map(key => this.scenario.configuration[key]);
        
        // 转换测试步骤
        this.scenario.sequence = this.scenario.sequence || [];
        this.scenario.sequence.forEach((step) => {
          step.parameters = step.parameters || {};
          step.expected = step.expected || {};
        });
      });
    },

    saveScenario() {
      if (!this.validateJson()) {
        return;
      }

      const scenario = {
        id: this.scenario.id,
        name: this.scenario.name,
        description: this.scenario.description,
        metadata: this.scenario.metadata,
        configuration: this.scenario.configuration,
        sequence: this.scenario.sequence.map((step) => ({
          action: step.action,
          parameters: step.parameters || {},
          timeout: step.timeout,
          expected: step.expected || null,
        })),
      };

      const action = this.isEdit
        ? this.$store.dispatch('updateScenario', scenario)
        : this.$store.dispatch('saveScenario', scenario);

      action
        .then(() => {
          this.$message.success('保存成功');
          this.$router.push('/');
        })
        .catch((error) => {
          this.$message.error('保存失败: ' + error.message);
        });
    },

    addStep() {
      this.scenario.sequence.push({
        action: 'WAIT_LOGIN',
        parameters: {},
        timeout: 5000,
        expected: {},
      });
    },

    removeStep(index) {
      this.scenario.sequence.splice(index, 1);
    },

    addMetadataItem() {
      const key = `key_${Date.now()}`;
      this.scenario.metadata[key] = '';
      this.metadataKeys.push(key);
      this.metadataValues.push('');
    },

    removeMetadataItem(key) {
      delete this.scenario.metadata[key];
      const index = this.metadataKeys.indexOf(key);
      if (index > -1) {
        this.metadataKeys.splice(index, 1);
        this.metadataValues.splice(index, 1);
      }
    },

    addConfigItem() {
      const key = `config_${Date.now()}`;
      this.scenario.configuration[key] = '';
      this.configKeys.push(key);
      this.configValues.push('');
    },

    removeConfigItem(key) {
      delete this.scenario.configuration[key];
      const index = this.configKeys.indexOf(key);
      if (index > -1) {
        this.configKeys.splice(index, 1);
        this.configValues.splice(index, 1);
      }
    },

    updateMetadata() {
      this.scenario.metadata = {};
      this.metadataKeys.forEach((key, index) => {
        if (key) {
          this.scenario.metadata[key] = this.metadataValues[index] || '';
        }
      });
    },

    updateConfiguration() {
      this.scenario.configuration = {};
      this.configKeys.forEach((key, index) => {
        if (key) {
          this.scenario.configuration[key] = this.configValues[index] || '';
        }
      });
    },

    getActionLabel(actionValue) {
      const action = this.availableActions.find(a => a.value === actionValue);
      return action ? action.label : actionValue;
    },

    getParameterFormComponent(action) {
      const componentMap = {
        'SEND_NEW_ORDER': 'new-order-form',
        'CANCEL_ORDER': 'cancel-order-form',
        'WAIT_EXECUTION_REPORT': 'execution-report-form',
      };
      return componentMap[action] || 'default-form';
    },

    getExpectedFormComponent(action) {
      const componentMap = {
        'WAIT_EXECUTION_REPORT': 'execution-report-form',
      };
      return componentMap[action] || 'default-form';
    },

    validateJson() {
      try {
        // 验证元数据
        JSON.stringify(this.scenario.metadata);
        
        // 验证配置
        JSON.stringify(this.scenario.configuration);
        
        // 验证每个步骤的参数和预期结果
        this.scenario.sequence.forEach((step) => {
          JSON.stringify(step.parameters || {});
          JSON.stringify(step.expected || {});
        });
        
        return true;
      } catch (e) {
        this.$message.error('数据格式错误: ' + e.message);
        return false;
      }
    },
  },
};
</script>

<style scoped>
.scenario-editor {
  padding: 20px;
}

.json-preview {
  background: #f5f5f5;
  padding: 10px;
  border-radius: 4px;
  max-height: 600px;
  overflow-y: auto;
  font-size: 12px;
}

.json-editor-card {
  margin-bottom: 10px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.json-item {
  display: flex;
  align-items: center;
  margin-bottom: 10px;
}

.empty-tip {
  color: #909399;
  font-size: 12px;
  text-align: center;
  padding: 20px;
}

.step-card {
  margin-bottom: 10px;
}

.step-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.step-item {
  margin-bottom: 15px;
}
</style>
