# 项目文档

项目维护以下当前文档和已验收的专题部署记录：

| 文档 | 内容 |
| --- | --- |
| `project-design.md` | 需求、架构、数据库、API、页面、设备接入和部署 |
| `project-status.md` | 当前已完成、验证结果、限制和下一步 |
| `development-log.md` | 开发过程、问题原因、修复方法和验证结果 |
| `future-development-backlog.md` | 后续开发计划和优先级 |
| `technical-decisions.md` | 技术选型、版本及选择原因 |
| `hivemq-remote-mqtt-version.md` | HiveMQ Cloud 远程 MQTT 分支的实现、边界、部署方式和验收要求 |
| `README.md` | 本导航 |
| `synology-nas-deployment.md` | 群晖 DSM Container Manager 实际部署、故障处理和验收记录 |

项目展示与发布辅助材料位于仓库根目录与 `screenshots/`：根目录 `README.md` 面向 GitHub 读者，`docs/architecture.mmd` 是可维护的 Mermaid 架构图源文件，`screenshots/README.md` 规定截图准备与脱敏要求，`SECURITY-CHECKLIST.md` 用于发布前安全复核。这些材料不替代上表六份项目事实文档。

## 阅读方式

新对话：

```text
先读取根目录 PROJECT_CONTEXT.md
再读取 docs/project-status.md
按任务需要读取其余文档
```

人工查看：

- 想知道系统怎么设计：`project-design.md`
- 想知道现在做到哪里：`project-status.md`
- 想查看报错与解决方法：`development-log.md`
- 想知道下一步做什么：`future-development-backlog.md`
- 想知道为什么选择这些技术：`technical-decisions.md`
- 想在群晖部署或排查群晖部署：`synology-nas-deployment.md`

## 原始历史

合并前的所有原始文档完整保存在：

```text
history/docs-before-consolidation-2026-07-09.zip
```

需要查看早期详细需求、数据库设计、API 设计、页面设计、Docker/MQTT 专题文档或原始长日志时，解压该文件即可。

其中 6 份主要原始设计文档也已恢复为可直接阅读的 Markdown：

```text
history/original-docs/
```

## 维护规则

- 不再为单个小功能新增 Markdown。
- 当前事实更新 `project-status.md`。
- 后续任务更新 `future-development-backlog.md`。
- 故障与修复更新 `development-log.md`。
- 技术选择变化更新 `technical-decisions.md`。
- 系统结构、接口、设备协议或部署方式变化更新 `project-design.md`。
