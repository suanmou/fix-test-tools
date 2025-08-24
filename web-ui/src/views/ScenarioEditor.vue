<template>
  <div class="scenario-editor">
    <el-row :gutter="20">
      <el-col :span="16">
        <el-card>
          <div slot="header">
            <span>{{ isEdit ? '编辑测试场景' : '创建测试场景' }}</span>
            <el-button-group style="float: right">
              <el-button type="success" @click="loadTemplate" icon="el-icon-document-copy">加载模板</el-button>
              <el-button type="primary" @click="saveScenario" icon="el-icon-check">保存</el-button>
              <el-button @click="$router.push('/')" icon="el-icon-back">返回</el-button>
            </el-button-group>
          </div>

          <el-form :model="scenario" label-width="120px">
            <el-form-item label="场景ID">
              <el-input v-model="scenario.id" :disabled="isEdit"></el-input>
            </el-form-item>

            <el-form-item label="场景名称">
              <el-input v-model="scenario.name"></el-input>
            </el-form-item>

            <el-form-item label="场景类型">
              <el-select v-model="scenario.type" placeholder="选择场景类型" style="width: 100%">
                <el-option label="功能测试" value="functional" />
                <el-option label="性能测试" value="performance" />
                <el-option label="压力测试" value="stress" />
                <el-option label="回归测试" value="regression" />
                <el-option label="边界测试" value="boundary" />
              </el-select>
            </el-form-item>

            <el-form-item label="优先级">
              <el-select v-model="scenario.priority" placeholder="选择优先级">
                <el-option label="高" value="high" />
                <el-option label="中" value="medium" />
                <el-option label="低" value="low" />
              </el-select>
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
                  <el-button type="text" @click="addMetadataItem" icon="el-icon-plus">添加字段</el-button>
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
                  <el-button type="text" @click="addConfigItem" icon="el-icon-plus">添加配置</el-button>
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

            <!-- 批量操作按钮 -->
            <el-button-group style="margin-bottom: 10px">
              <el-button type="primary" icon="el-icon-plus" @click="addStep">添加步骤</el-button>
              <el-button @click="clearAllSteps" icon="el-icon-delete">清空步骤</el-button>
              <el-button @click="sortSteps" icon="el-icon-sort">排序步骤</el-button>
            </el-button-group>

            <!-- 测试步骤的动态表单 -->
            <div v-for="(step, index) in scenario.sequence" :key="index" class="step-card">
              <el-card shadow="hover" class="step-item">
                <div slot="header" class="step-header">
                  <span>步骤 {{ index + 1 }}: {{ getActionLabel(step.action) }}</span>
                  <div>
                    <el-button type="text" icon="el-icon-top" @click="moveStepUp(index)" :disabled="index === 0" />
                    <el-button type="text" icon="el-icon-bottom" @click="moveStepDown(index)" :disabled="index === scenario.sequence.length - 1" />
                    <el-button type="danger" icon="el-icon-delete" @click="removeStep(index)" />
                  </div>
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

                  <el-form-item label="重试次数">
                    <el-input-number
                      v-model="step.retryCount"
                      :min="0"
                      :max="10"
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
          </el-form>
        </el-card>
      </el-col>

      <el-col :span="8">
        <el-card>
          <div slot="header">
            <span>JSON预览</span>
            <el-button-group style="float: right">
              <el-button type="text" @click="validateJson" icon="el-icon-check">验证</el-button>
              <el-button type="text" @click="formatJson" icon="el-icon-brush">格式化</el-button>
              <el-button type="text" @click="copyJson" icon="el-icon-document-copy">复制</el-button>
            </el-button-group>
          </div>
          <pre class="json-preview">{{ jsonPreview }}</pre>
        </el-card>

        <!-- 场景模板面板 -->
        <el-card style="margin-top: 20px">
          <div slot="header">
            <span>场景模板</span>
          </div>
          <el-collapse v-model="activeTemplate">
            <el-collapse-item 
              v-for="(template, key) in scenarioTemplates" 
              :key="key" 
              :title="template.name" 
              :name="key"
            >
              <p>{{ template.description }}</p>
              <el-button type="text" @click="applyTemplate(key)" icon="el-icon-document">应用模板</el-button>
            </el-collapse-item>
          </el-collapse>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script>
// 引入所有动态表单组件
import WaitLoginForm from '@/components/WaitLoginForm.vue'
import NewOrderForm from '@/components/NewOrderForm.vue'
import CancelOrderForm from '@/components/CancelOrderForm.vue'
import ModifyOrderForm from '@/components/ModifyOrderForm.vue'
import ExecutionReportForm from '@/components/ExecutionReportForm.vue'
import HeartbeatForm from '@/components/HeartbeatForm.vue'
import QueryStatusForm from '@/components/QueryStatusForm.vue'
import RequestMarketDataForm from '@/components/RequestMarketDataForm.vue'
import RiskCheckForm from '@/components/RiskCheckForm.vue'
import DefaultForm from '@/components/DefaultForm.vue'

export default {
  name: 'ScenarioEditor',
  components: {
    WaitLoginForm,
    NewOrderForm,
    CancelOrderForm,
    ModifyOrderForm,
    ExecutionReportForm,
    HeartbeatForm,
    QueryStatusForm,
    RequestMarketDataForm,
    RiskCheckForm,
    DefaultForm
  },
  data() {
    return {
      scenario: {
        id: '',
        name: '',
        type: 'functional',
        priority: 'medium',
        description: '',
        metadata: {},
        configuration: {},
        sequence: [],
      },
      metadataKeys: [],
      metadataValues: [],
      configKeys: [],
      configValues: [],
      activeTemplate: [],
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
        { value: 'REQUEST_MARKET_DATA', label: '请求市场数据' },
        { value: 'RISK_CHECK', label: '风险检查' },
      ],
      scenarioTemplates: {
        'basic-login': {
          name: '基础登录场景',
          description: '包含登录验证的基础测试场景',
          scenario: {
            name: '基础登录测试',
            description: '测试用户登录功能的完整流程',
            type: 'functional',
            priority: 'high',
            metadata: { testType: 'login', environment: 'staging' },
            configuration: { timeout: 5000, retryAttempts: 3 },
            sequence: [
              {
                action: 'WAIT_LOGIN',
                parameters: { waitTime: 30, username: 'test_user', password: 'test_pass' },
                timeout: 5000,
                retryCount: 3,
                expected: { success: true }
              }
            ]
          }
        },
        'order-lifecycle': {
          name: '订单生命周期',
          description: '完整的订单创建、修改、取消流程测试',
          scenario: {
            name: '订单生命周期测试',
            description: '测试订单从创建到完成的完整生命周期',
            type: 'functional',
            priority: 'high',
            metadata: { testType: 'order_flow', environment: 'staging' },
            configuration: { timeout: 10000, retryAttempts: 2 },
            sequence: [
              {
                action: 'SEND_NEW_ORDER',
                parameters: {
                  clOrdID: 'TEST001',
                  symbol: 'AAPL',
                  side: '1',
                  orderQty: 100,
                  price: 150.00,
                  ordType: '2',
                  timeInForce: '0'
                },
                timeout: 5000,
                retryCount: 2,
                expected: { ordStatus: '0' }
              },
              {
                action: 'MODIFY_ORDER',
                parameters: {
                  origClOrdID: 'TEST001',
                  clOrdID: 'TEST001_MOD',
                  symbol: 'AAPL',
                  orderQty: 150,
                  price: 155.00,
                  modifyType: 'ALL'
                },
                timeout: 5000,
                retryCount: 2,
                expected: { ordStatus: '5' }
              },
              {
                action: 'CANCEL_ORDER',
                parameters: {
                  origClOrdID: 'TEST001_MOD',
                  clOrdID: 'TEST001_CANCEL',
                  symbol: 'AAPL',
                  orderQty: 150,
                  cancelReason: 'U'
                },
                timeout: 5000,
                retryCount: 2,
                expected: { ordStatus: '4' }
              }
            ]
          }
        },
        'market-data': {
          name: '市场数据测试',
          description: '测试市场数据请求和处理',
          scenario: {
            name: '市场数据测试',
            description: '测试实时市场数据的获取和处理',
            type: 'performance',
            priority: 'medium',
            metadata: { testType: 'market_data', symbols: 'AAPL,TSLA,MSFT' },
            configuration: { timeout: 3000, retryAttempts: 1 },
            sequence: [
              {
                action: 'REQUEST_MARKET_DATA',
                parameters: {
                  symbol: 'AAPL',
                  dataType: 'LIVE',
                  fields: ['BID', 'ASK', 'LAST', 'VOLUME']
                },
                timeout: 3000,
                retryCount: 1,
                expected: { success: true }
              }
            ]
          }
        },
        'risk-management': {
          name: '风险管理测试',
          description: '测试各种风险控制规则',
          scenario: {
            name: '风险管理测试',
            description: '测试交易系统的风险控制功能',
            type: 'boundary',
            priority: 'high',
            metadata: { testType: 'risk_management', rules: 'position,price,quantity' },
            configuration: { timeout: 2000, retryAttempts: 1 },
            sequence: [
              {
                action: 'RISK_CHECK',
                parameters: {
                  riskType: 'POSITION',
                  symbol: 'AAPL',
                  parameters: '{"maxPosition": 1000}'
                },
                timeout: 2000,
                retryCount: 1,
                expected: { allowed: true }
              }
            ]
          }
        }
      }
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
        type: this.scenario.type,
        priority: this.scenario.priority,
        description: this.scenario.description,
        metadata: this.scenario.metadata,
        configuration: this.scenario.configuration,
        sequence: this.scenario.sequence.map((step) => ({
          action: step.action,
          parameters: step.parameters || {},
          timeout: step.timeout || 5000,
          retryCount: step.retryCount || 0,
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
        type: this.scenario.type,
        priority: this.scenario.priority,
        description: this.scenario.description,
        metadata: this.scenario.metadata,
        configuration: this.scenario.configuration,
        sequence: this.scenario.sequence.map((step) => ({
          action: step.action,
          parameters: step.parameters || {},
          timeout: step.timeout || 5000,
          retryCount: step.retryCount || 0,
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
        retryCount: 0,
        expected: {},
      });
    },

    removeStep(index) {
      this.scenario.sequence.splice(index, 1);
    },

    moveStepUp(index) {
      if (index > 0) {
        const temp = this.scenario.sequence[index];
        this.scenario.sequence.splice(index, 1);
        this.scenario.sequence.splice(index - 1, 0, temp);
      }
    },

    moveStepDown(index) {
      if (index < this.scenario.sequence.length - 1) {
        const temp = this.scenario.sequence[index];
        this.scenario.sequence.splice(index, 1);
        this.scenario.sequence.splice(index + 1, 0, temp);
      }
    },

    clearAllSteps() {
      this.$confirm('确定要清空所有步骤吗？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        this.scenario.sequence = [];
      });
    },

    sortSteps() {
      // 按操作类型排序
      const order = ['WAIT_LOGIN', 'SEND_HEARTBEAT', 'SEND_TEST_REQUEST', 'SEND_NEW_ORDER', 'CANCEL_ORDER', 'MODIFY_ORDER'];
      this.scenario.sequence.sort((a, b) => {
        const indexA = order.indexOf(a.action);
        const indexB = order.indexOf(b.action);
        if (indexA === -1) return 1;
        if (indexB === -1) return -1;
        return indexA - indexB;
      });
    },

    // 元数据和配置管理
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

    // 组件映射
    getActionLabel(actionValue) {
      const action = this.availableActions.find(a => a.value === actionValue);
      return action ? action.label : actionValue;
    },

    getParameterFormComponent(action) {
      const componentMap = {
        'WAIT_LOGIN': 'WaitLoginForm',
        'SEND_HEARTBEAT': 'HeartbeatForm',
        'SEND_TEST_REQUEST': 'HeartbeatForm',
        'SEND_NEW_ORDER': 'NewOrderForm',
        'CANCEL_ORDER': 'CancelOrderForm',
        'MODIFY_ORDER': 'ModifyOrderForm',
        'QUERY_ORDER_STATUS': 'QueryStatusForm',
        'WAIT_EXECUTION_REPORT': 'ExecutionReportForm',
        'WAIT_ORDER_CANCEL_RESPONSE': 'ExecutionReportForm',
        'WAIT_ORDER_MODIFY_RESPONSE': 'ExecutionReportForm',
        'REQUEST_MARKET_DATA': 'RequestMarketDataForm',
        'RISK_CHECK': 'RiskCheckForm',
      };
      return componentMap[action] || 'DefaultForm';
    },

    getExpectedFormComponent(action) {
      const componentMap = {
        'WAIT_LOGIN': 'DefaultForm',
        'SEND_HEARTBEAT': 'DefaultForm',
        'SEND_TEST_REQUEST': 'DefaultForm',
        'SEND_NEW_ORDER': 'ExecutionReportForm',
        'CANCEL_ORDER': 'ExecutionReportForm',
        'MODIFY_ORDER': 'ExecutionReportForm',
        'QUERY_ORDER_STATUS': 'DefaultForm',
        'WAIT_EXECUTION_REPORT': 'ExecutionReportForm',
        'WAIT_ORDER_CANCEL_RESPONSE': 'ExecutionReportForm',
        'WAIT_ORDER_MODIFY_RESPONSE': 'ExecutionReportForm',
        'REQUEST_MARKET_DATA': 'DefaultForm',
        'RISK_CHECK': 'DefaultForm',
      };
      return componentMap[action] || 'DefaultForm';
    },

    // 模板管理
    loadTemplate() {
      this.$prompt('请输入模板名称', '加载模板', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        inputPattern: /^[a-zA-Z0-9_-]+$/,
        inputErrorMessage: '模板名称格式不正确'
      }).then(({ value }) => {
        if (this.scenarioTemplates[value]) {
          this.applyTemplate(value);
        } else {
          this.$message.error('模板不存在');
        }
      });
    },

    applyTemplate(templateKey) {
      const template = this.scenarioTemplates[templateKey];
      if (template) {
        this.$confirm(`确定要应用"${template.name}"模板吗？当前内容将被替换。`, '提示', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        }).then(() => {
          this.scenario = {
            ...this.scenario,
            ...template.scenario
          };
          
          // 重新初始化元数据和配置
          this.metadataKeys = Object.keys(this.scenario.metadata || {});
          this.metadataValues = this.metadataKeys.map(key => this.scenario.metadata[key]);
          this.configKeys = Object.keys(this.scenario.configuration || {});
          this.configValues = this.configKeys.map(key => this.scenario.configuration[key]);
          
          this.$message.success('模板已应用');
        });
      }
    },

    // JSON操作
    validateJson() {
      try {
        JSON.stringify(this.scenario);
        this.$message.success('JSON格式正确');
        return true;
      } catch (e) {
        this.$message.error('JSON格式错误: ' + e.message);
        return false;
      }
    },

    formatJson() {
      try {
        const formatted = JSON.stringify(JSON.parse(this.jsonPreview), null, 2);
        this.$message.success('JSON已格式化');
      } catch (e) {
        this.$message.error('格式化失败: ' + e.message);
      }
    },

    copyJson() {
      navigator.clipboard.writeText(this.jsonPreview).then(() => {
        this.$message.success('JSON已复制到剪贴板');
      }).catch(() => {
        this.$message.error('复制失败');
      });
    }
  },
};
</script>

<style scoped>
.scenario-editor {
  padding: 20px;
}

.json-preview {
  background: #f5f5f5;
  padding: 15px;
  border-radius: 4px;
  max-height: 600px;
  overflow-y: auto;
  font-size: 12px;
  font-family: 'Consolas', 'Monaco', monospace;
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
  margin-bottom: 15px;
}

.step-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.step-item {
  margin-bottom: 15px;
}

.el-form-item {
  margin-bottom: 15px;
}

.el-form-item:last-child {
  margin-bottom: 0;
}
</style>