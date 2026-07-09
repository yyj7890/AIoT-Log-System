# AIoT 智能设备运行日志管理系统 - 前端页面结构设计

## 1. 设计目标

前端用于支撑第一阶段功能：

- 首页统计
- 设备管理
- 设备详情
- 日志管理
- 标签管理
- 日志筛选

推荐技术栈：

- Vue 3
- TypeScript
- Vue Router
- Pinia
- Element Plus
- Axios

第一阶段页面应以管理系统为主，界面保持清晰、实用、便于演示。

## 2. 前端目录结构

建议目录：

```text
frontend
├── src
│   ├── api
│   │   ├── dashboard.ts
│   │   ├── devices.ts
│   │   ├── logs.ts
│   │   ├── tags.ts
│   │   └── enums.ts
│   ├── assets
│   ├── components
│   │   ├── AppHeader.vue
│   │   ├── AppSidebar.vue
│   │   ├── DeviceFormDialog.vue
│   │   ├── LogFormDialog.vue
│   │   ├── StatusTag.vue
│   │   └── PageContainer.vue
│   ├── layouts
│   │   └── MainLayout.vue
│   ├── router
│   │   └── index.ts
│   ├── stores
│   │   └── enumStore.ts
│   ├── types
│   │   ├── api.ts
│   │   ├── device.ts
│   │   ├── log.ts
│   │   └── tag.ts
│   ├── views
│   │   ├── DashboardView.vue
│   │   ├── DeviceListView.vue
│   │   ├── DeviceDetailView.vue
│   │   ├── LogListView.vue
│   │   └── TagListView.vue
│   ├── App.vue
│   └── main.ts
├── index.html
├── package.json
├── tsconfig.json
└── vite.config.ts
```

## 3. 路由设计

| 路径 | 页面 | 说明 |
| --- | --- | --- |
| `/` | 重定向到 `/dashboard` | 默认进入首页 |
| `/dashboard` | DashboardView | 首页统计 |
| `/devices` | DeviceListView | 设备列表 |
| `/devices/:id` | DeviceDetailView | 设备详情 |
| `/logs` | LogListView | 日志列表 |
| `/tags` | TagListView | 标签管理 |

路由结构建议：

```ts
const routes = [
  {
    path: '/',
    component: MainLayout,
    children: [
      { path: '', redirect: '/dashboard' },
      { path: 'dashboard', component: DashboardView },
      { path: 'devices', component: DeviceListView },
      { path: 'devices/:id', component: DeviceDetailView },
      { path: 'logs', component: LogListView },
      { path: 'tags', component: TagListView }
    ]
  }
]
```

## 4. 整体布局

### 4.1 MainLayout

后台管理布局：

- 左侧导航栏
- 顶部标题栏
- 右侧主内容区

导航菜单：

- 首页
- 设备管理
- 日志管理
- 标签管理

顶部标题栏显示：

- 系统名称：AIoT 智能设备运行日志管理系统
- 当前页面标题

## 5. 首页 DashboardView

首页目标：快速查看设备整体运行状态和最近日志。

### 5.1 页面区块

页面包含：

- 统计卡片区
- 最近异常日志
- 最近维护记录

### 5.2 统计卡片

卡片内容：

- 设备总数
- 正常设备数
- 异常设备数
- 离线设备数
- 维护中设备数
- 待处理日志数

对应接口：

```http
GET /api/dashboard/summary
```

### 5.3 最近异常日志

展示字段：

- 日志标题
- 所属设备
- 日志等级
- 处理状态
- 创建时间

操作：

- 点击日志进入日志详情或打开详情弹窗
- 点击设备进入设备详情

### 5.4 最近维护记录

展示字段：

- 日志标题
- 所属设备
- 处理状态
- 创建时间

## 6. 设备列表 DeviceListView

设备列表用于管理设备基础信息。

### 6.1 筛选区

筛选字段：

- 关键词，匹配设备名称或设备编号
- 设备类型
- 设备状态

操作按钮：

- 查询
- 重置
- 新增设备

对应接口：

```http
GET /api/devices
```

### 6.2 表格字段

表格列：

- 设备名称
- 设备编号
- 设备类型
- 安装位置
- 当前状态
- 最后在线时间
- 创建时间
- 操作

操作：

- 查看详情
- 编辑
- 删除
- 添加日志

### 6.3 新增和编辑设备

建议使用弹窗组件：

```text
DeviceFormDialog.vue
```

表单字段：

- 设备名称
- 设备编号，新增时填写，编辑时禁用
- 设备类型
- 安装位置
- 当前状态
- 设备描述

新增接口：

```http
POST /api/devices
```

编辑接口：

```http
PUT /api/devices/{id}
```

删除接口：

```http
DELETE /api/devices/{id}
```

## 7. 设备详情 DeviceDetailView

设备详情用于查看某台设备的基础信息和相关日志。

### 7.1 页面区块

页面包含：

- 设备基础信息
- 设备日志统计
- 最近日志列表

对应接口：

```http
GET /api/devices/{id}
```

### 7.2 设备基础信息

展示字段：

- 设备名称
- 设备编号
- 设备类型
- 安装位置
- 当前状态
- 最后在线时间
- 设备描述

### 7.3 设备日志统计

展示字段：

- 日志总数
- 异常日志数
- 待处理日志数

### 7.4 最近日志列表

展示字段：

- 日志标题
- 日志类型
- 日志等级
- 处理状态
- 标签
- 创建时间

操作：

- 新增日志
- 查看全部日志
- 编辑日志状态

查看全部日志时跳转：

```text
/logs?deviceId={id}
```

## 8. 日志列表 LogListView

日志列表用于查看和筛选全部设备日志。

### 8.1 筛选区

筛选字段：

- 所属设备
- 日志类型
- 日志等级
- 处理状态
- 标签
- 关键词
- 时间范围

操作按钮：

- 查询
- 重置
- 新增日志

对应接口：

```http
GET /api/logs
```

### 8.2 表格字段

表格列：

- 日志标题
- 所属设备
- 日志类型
- 日志等级
- 处理状态
- 标签
- 来源
- 创建时间
- 操作

操作：

- 查看详情
- 编辑
- 修改状态
- 删除

### 8.3 新增和编辑日志

建议使用弹窗组件：

```text
LogFormDialog.vue
```

表单字段：

- 所属设备
- 日志标题
- 日志内容
- 日志类型
- 日志等级
- 处理状态
- 标签

新增接口：

```http
POST /api/logs
```

编辑接口：

```http
PUT /api/logs/{id}
```

修改状态接口：

```http
PATCH /api/logs/{id}/status
```

删除接口：

```http
DELETE /api/logs/{id}
```

## 9. 标签管理 TagListView

标签管理用于维护日志标签。

### 9.1 页面结构

页面包含：

- 标签列表
- 新增标签输入框

### 9.2 列表字段

字段：

- 标签名称
- 创建时间
- 操作

操作：

- 新增标签
- 删除标签

接口：

```http
GET /api/tags
POST /api/tags
DELETE /api/tags/{id}
```

## 10. 公共组件设计

### 10.1 StatusTag

用于展示设备状态、日志等级、日志处理状态。

输入：

- `type`：状态类型，例如 deviceStatus、logLevel、logStatus
- `value`：状态编码

输出：

- 中文标签
- 对应颜色

颜色建议：

| 类型 | 编码 | 颜色 |
| --- | --- | --- |
| 设备状态 | NORMAL | 绿色 |
| 设备状态 | ABNORMAL | 红色 |
| 设备状态 | OFFLINE | 灰色 |
| 设备状态 | MAINTENANCE | 蓝色 |
| 日志等级 | INFO | 蓝色 |
| 日志等级 | WARNING | 橙色 |
| 日志等级 | ERROR | 红色 |
| 日志状态 | PENDING | 橙色 |
| 日志状态 | PROCESSING | 蓝色 |
| 日志状态 | RESOLVED | 绿色 |

### 10.2 DeviceFormDialog

设备新增和编辑共用弹窗。

Props：

- `visible`
- `mode`
- `device`

Events：

- `success`
- `cancel`

### 10.3 LogFormDialog

日志新增和编辑共用弹窗。

Props：

- `visible`
- `mode`
- `log`
- `defaultDeviceId`

Events：

- `success`
- `cancel`

## 11. API 文件设计

### 11.1 dashboard.ts

方法：

- `getDashboardSummary()`

### 11.2 devices.ts

方法：

- `getDeviceList(params)`
- `getDeviceDetail(id)`
- `createDevice(data)`
- `updateDevice(id, data)`
- `deleteDevice(id)`

### 11.3 logs.ts

方法：

- `getLogList(params)`
- `getLogDetail(id)`
- `createLog(data)`
- `updateLog(id, data)`
- `updateLogStatus(id, status)`
- `deleteLog(id)`

### 11.4 tags.ts

方法：

- `getTagList()`
- `createTag(data)`
- `deleteTag(id)`

### 11.5 enums.ts

方法：

- `getEnums()`

## 12. TypeScript 类型设计

### 12.1 Device

```ts
export interface Device {
  id: number
  name: string
  deviceCode: string
  type: string
  location?: string
  status: DeviceStatus
  description?: string
  lastOnlineAt?: string
  createdAt: string
  updatedAt: string
}
```

### 12.2 Log

```ts
export interface Log {
  id: number
  deviceId: number
  deviceName: string
  deviceCode: string
  title: string
  content: string
  logType: LogType
  level: LogLevel
  status: LogStatus
  source: LogSource
  tags: Tag[]
  createdAt: string
  updatedAt: string
}
```

### 12.3 Tag

```ts
export interface Tag {
  id: number
  name: string
  createdAt?: string
  updatedAt?: string
}
```

## 13. 开发顺序建议

前端实现建议顺序：

1. 搭建 Vue 3 + TypeScript + Element Plus 项目
2. 创建基础布局 MainLayout
3. 配置路由
4. 封装 Axios
5. 编写枚举和状态标签组件
6. 实现设备列表和设备表单
7. 实现日志列表和日志表单
8. 实现首页统计
9. 实现设备详情
10. 实现标签管理

## 14. 与后端接口对应关系

| 前端页面 | 后端接口 |
| --- | --- |
| DashboardView | `GET /api/dashboard/summary` |
| DeviceListView | `GET /api/devices`, `POST /api/devices`, `PUT /api/devices/{id}`, `DELETE /api/devices/{id}` |
| DeviceDetailView | `GET /api/devices/{id}`, `GET /api/logs?deviceId={id}` |
| LogListView | `GET /api/logs`, `POST /api/logs`, `PUT /api/logs/{id}`, `PATCH /api/logs/{id}/status`, `DELETE /api/logs/{id}` |
| TagListView | `GET /api/tags`, `POST /api/tags`, `DELETE /api/tags/{id}` |

