<template>
  <div class="scenario-manager">
    <el-row :gutter="20">
      <el-col :span="24">
        <el-card>
          <div slot="header" class="clearfix">
            <span>测试场景管理</span>
            <el-button 
              type="primary" 
              icon="el-icon-plus" 
              style="float: right;" 
              @click="createNewScenario">
              创建新场景
            </el-button>
          </div>
          
          <el-table :data="scenarios" style="width: 100%">
            <el-table-column prop="id" label="ID" width="180"></el-table-column>
            <el-table-column prop="name" label="场景名称"></el-table-column>
            <el-table-column prop="description" label="描述"></el-table-column>
            <el-table-column label="操作" width="200">
              <template slot-scope="scope">
                <el-button 
                  size="mini" 
                  @click="editScenario(scope.row)">
                  编辑
                </el-button>
                <el-button 
                  size="mini" 
                  type="danger" 
                  @click="deleteScenario(scope.row)">
                  删除
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>

    <!-- 上传JSON对话框 -->
    <el-dialog title="上传JSON场景文件" :visible.sync="uploadDialogVisible">
      <el-upload
        class="upload-demo"
        drag
        action="/api/scenarios/upload"
        accept=".json"
        :on-success="handleUploadSuccess"
        :on-error="handleUploadError"
        :before-upload="beforeUpload">
        <i class="el-icon-upload"></i>
        <div class="el-upload__text">将文件拖到此处，或<em>点击上传</em></div>
        <div class="el-upload__tip" slot="tip">只能上传JSON文件</div>
      </el-upload>
    </el-dialog>
  </div>
</template>

<script>
export default {
  name: 'ScenarioManager',
  data() {
    return {
      uploadDialogVisible: false
    }
  },
  computed: {
    scenarios() {
      return this.$store.state.scenarios
    }
  },
  created() {
    this.loadScenarios()
  },
  methods: {
    loadScenarios() {
      this.$store.dispatch('loadScenarios')
    },
    
    createNewScenario() {
      this.$router.push('/editor')
    },
    
    editScenario(scenario) {
      this.$router.push(`/editor/${scenario.id}`)
    },
    
    deleteScenario(scenario) {
      this.$confirm('确认删除该测试场景?', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        this.$store.dispatch('deleteScenario', scenario.id)
          .then(() => {
            this.$message.success('删除成功')
          })
      }).catch(() => {
        this.$message.info('已取消删除')
      })
    },
    
    showUploadDialog() {
      this.uploadDialogVisible = true
    },
    
    handleUploadSuccess(response) {
      this.$message.success('上传成功')
      this.uploadDialogVisible = false
      this.loadScenarios()
    },
    
    handleUploadError(error) {
      this.$message.error('上传失败: ' + error.message)
    },
    
    beforeUpload(file) {
      const isJson = file.type === 'application/json'
      if (!isJson) {
        this.$message.error('只能上传JSON文件!')
      }
      return isJson
    }
  }
}
</script>

<style scoped>
.scenario-manager {
  padding: 20px;
}
</style>