# 项目文档导航

新对话先读取根目录 `PROJECT_CONTEXT.md`。日常开发通常只需要本文件和 `project-status.md`。

## 常用文档

| 文档 | 用途 | 何时阅读 |
| --- | --- | --- |
| `project-status.md` | 当前完成情况、验证状态、下一步 | 每次继续项目 |
| `future-development-backlog.md` | 后续开发优先级 | 选择下一项任务 |
| `docker-deployment-guide.md` | Docker 启动、更新、停止和排错 | 部署与交付 |
| `mqtt-reporting-guide.md` | MQTT Topic、数据格式、真实设备连接 | 设备联调 |

## 设计参考

以下文档保留独立职责，不要求新对话默认全部读取：

| 文档 | 内容 |
| --- | --- |
| `phase-1-requirements.md` | 第一阶段原始需求和验收标准 |
| `database-design.md` | 表结构、关系、索引和字段说明 |
| `backend-api-design.md` | HTTP API 路径、参数和响应 |
| `frontend-page-design.md` | 页面、路由、组件和交互设计 |
| `technical-decisions.md` | 技术选型及原因 |
| `development-log.md` | 按日期整理的里程碑、故障原因和修复方法 |

## 历史归档

`archive/` 保存已完成、已被替代或重写前的文档，不作为当前开发依据：

- 已完成的后端实施计划
- 容易过期的后端文件地图
- 重写前的项目状态、路线图和文档索引
- 整理前的完整开发日志

归档内容没有删除，需要追溯早期过程时仍可查看。

## 维护规则

- 当前事实只更新 `PROJECT_CONTEXT.md` 和 `project-status.md`。
- 未来任务只更新 `future-development-backlog.md`。
- API、数据库、前端或部署发生变化时，只更新对应专题文档。
- 故障处理结果追加到 `development-log.md`，不要重复写入所有文档。
