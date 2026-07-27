package com.aiot.log.controller;
import com.aiot.log.common.ApiResponse;
import com.aiot.log.dto.McpToolExecutionRequest;
import com.aiot.log.entity.McpToolExecution;
import com.aiot.log.mapper.McpToolExecutionMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import java.util.List;
@RestController @RequestMapping("/api/mcp-tool-executions") @Tag(name="AI MCP 操作记录")
public class McpToolExecutionController {
    private final McpToolExecutionMapper mapper;
    public McpToolExecutionController(McpToolExecutionMapper mapper){this.mapper=mapper;}
    @GetMapping public ApiResponse<List<McpToolExecution>> list(@RequestParam(defaultValue="30") int limit){
        return ApiResponse.success(mapper.selectList(new LambdaQueryWrapper<McpToolExecution>().orderByDesc(McpToolExecution::getCreatedAt).last("LIMIT " + Math.min(Math.max(limit,1),100))));
    }
    @PostMapping public ApiResponse<McpToolExecution> record(@Valid @RequestBody McpToolExecutionRequest request){
        McpToolExecution item=new McpToolExecution(); item.setToolName(request.getToolName()); item.setTargetSummary(request.getTargetSummary()); item.setStatus(request.getStatus()); item.setResultSummary(request.getResultSummary()); mapper.insert(item); return ApiResponse.success(item);
    }
}
