package com.aiot.log.dto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
public class McpToolExecutionRequest {
    @NotBlank @Size(max=100) private String toolName;
    @Size(max=300) private String targetSummary;
    @NotBlank @Pattern(regexp="SUCCEEDED|FAILED") private String status;
    @Size(max=500) private String resultSummary;
    public String getToolName(){return toolName;} public void setToolName(String v){toolName=v;}
    public String getTargetSummary(){return targetSummary;} public void setTargetSummary(String v){targetSummary=v;}
    public String getStatus(){return status;} public void setStatus(String v){status=v;}
    public String getResultSummary(){return resultSummary;} public void setResultSummary(String v){resultSummary=v;}
}
