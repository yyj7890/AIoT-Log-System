import { request } from './http'
export interface McpToolExecution { id:number; toolName:string; targetSummary?:string; status:'SUCCEEDED'|'FAILED'; resultSummary?:string; createdAt:string }
export function getMcpToolExecutions(limit=30){ return request<McpToolExecution[]>({url:'/mcp-tool-executions',method:'GET',params:{limit}}) }
