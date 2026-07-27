CREATE TABLE mcp_tool_executions (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tool_name VARCHAR(100) NOT NULL,
  target_summary VARCHAR(300) NULL,
  status VARCHAR(16) NOT NULL,
  result_summary VARCHAR(500) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_mcp_tool_executions_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI MCP 工具调用脱敏记录';
