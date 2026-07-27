package com.aiot.log.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("mcp_tool_executions")
public class McpToolExecution {
    private Long id; private String toolName; private String targetSummary; private String status; private String resultSummary; private LocalDateTime createdAt;
    public Long getId() { return id; } public void setId(Long id) { this.id = id; }
    public String getToolName() { return toolName; } public void setToolName(String toolName) { this.toolName = toolName; }
    public String getTargetSummary() { return targetSummary; } public void setTargetSummary(String targetSummary) { this.targetSummary = targetSummary; }
    public String getStatus() { return status; } public void setStatus(String status) { this.status = status; }
    public String getResultSummary() { return resultSummary; } public void setResultSummary(String resultSummary) { this.resultSummary = resultSummary; }
    public LocalDateTime getCreatedAt() { return createdAt; } public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
