# FIX测试工具Web界面

基于Vue2 + ElementUI的测试场景管理界面。

## 功能特性

- 📁 测试场景文件管理
- 📝 可视化场景编辑器
- 📤 JSON文件上传
- 🔍 场景预览和验证
- 🎯 支持所有测试操作类型

## 快速开始

### 安装依赖
```bash
cd web-ui
npm install
```

### 启动开发服务器
```bash
npm run dev
```

### 构建生产版本
```bash
npm run build
```

## API接口

- `GET /api/scenarios` - 获取所有场景
- `GET /api/scenarios/{id}` - 获取特定场景
- `POST /api/scenarios` - 创建新场景
- `PUT /api/scenarios/{id}` - 更新场景
- `DELETE /api/scenarios/{id}` - 删除场景
- `POST /api/scenarios/upload` - 上传JSON文件

## 使用说明

1. 访问 `http://localhost:8080` 打开管理界面
2. 点击"创建新场景"或上传JSON文件
3. 在编辑器中配置场景参数
4. 保存并运行测试

## 支持的测试操作

- WAIT_LOGIN: 等待登录
- SEND_HEARTBEAT: 发送心跳
- SEND_TEST_REQUEST: 发送测试请求
- SEND_NEW_ORDER: 发送新订单
- CANCEL_ORDER: 取消订单
- MODIFY_ORDER: 修改订单
- QUERY_ORDER_STATUS: 查询订单状态
- WAIT_EXECUTION_REPORT: 等待执行报告
- WAIT_ORDER_CANCEL_RESPONSE: 等待取消响应
- WAIT_ORDER_MODIFY_RESPONSE: 等待修改响应