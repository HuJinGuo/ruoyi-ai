/**
 * 工具调用信息类型定义
 */
export interface ToolCallInfo {
  /** 唯一标识 key */
  key?: number;
  /** 工具名称 */
  name: string;
  /** 调用状态: pending-调用中, success-成功, error-失败 */
  status: 'pending' | 'success' | 'error';
  /** 调用结果 */
  result: unknown;
  /** 调用时间戳 */
  timestamp: number;
  /** 类型：AGENT / TOOL / GUARD / MODEL / LOG */
  type?: string;
  /** 运行 ID */
  runId?: string;
  /** 步骤 ID */
  stepId?: string;
  /** 调用输入 */
  input?: unknown;
  /** 前端展示顺序 */
  order?: number;
}
