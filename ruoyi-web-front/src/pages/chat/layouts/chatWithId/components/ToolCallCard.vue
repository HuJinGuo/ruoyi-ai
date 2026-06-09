<script setup lang="ts">
import type { ToolCallInfo } from '@/pages/chat/layouts/chatWithId/types';

const props = defineProps<{
  toolInfo: ToolCallInfo;
}>();

const emit = defineEmits<{
  confirmDraft: [draftId: string];
}>();

const isExpanded = ref(false);

// 状态图标和颜色映射
const statusConfig = computed(() => {
  switch (props.toolInfo.status) {
    case 'pending':
      return {
        icon: 'Loading',
        color: '#E6A23C',
        bgColor: 'rgba(230, 162, 60, 0.1)',
        text: '调用中',
      };
    case 'success':
      return {
        icon: 'CircleCheckFilled',
        color: '#67C23A',
        bgColor: 'rgba(103, 194, 58, 0.1)',
        text: '成功',
      };
    case 'error':
      return {
        icon: 'CircleCloseFilled',
        color: '#F56C6C',
        bgColor: 'rgba(245, 108, 108, 0.1)',
        text: '失败',
      };
    default:
      return {
        icon: 'QuestionFilled',
        color: '#909399',
        bgColor: 'rgba(144, 147, 153, 0.1)',
        text: '未知',
      };
  }
});

// 格式化时间
const formattedTime = computed(() => {
  if (!props.toolInfo.timestamp)
    return '';
  const date = new Date(props.toolInfo.timestamp);
  return date.toLocaleTimeString('zh-CN', {
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
  });
});

// 格式化结果内容（用于展示）
const formattedResult = computed(() => {
  return formatPanelValue(props.toolInfo.result);
});

const formattedInput = computed(() => {
  return formatPanelValue(props.toolInfo.input);
});

const resultSummary = computed(() => {
  if (props.toolInfo.status === 'pending' && !props.toolInfo.result) {
    return '等待返回结果';
  }

  const summary = summarizeValue(props.toolInfo.result);
  if (summary) {
    return summary;
  }

  return props.toolInfo.status === 'success' ? '执行完成，未返回详细结果' : '暂无返回结果';
});

const typeText = computed(() => {
  switch (props.toolInfo.type) {
    case 'AGENT':
      return 'Agent';
    case 'TOOL':
      return '工具';
    case 'GUARD':
      return '护栏';
    case 'MODEL':
      return '模型';
    case 'LOG':
      return '日志';
    default:
      return props.toolInfo.type || '步骤';
  }
});

function formatPanelValue(value: unknown) {
  if (value === undefined || value === null || value === '')
    return null;
  try {
    if (typeof value === 'string') {
      const trimmed = value.trim();
      if (!trimmed) {
        return null;
      }
      if ((trimmed.startsWith('{') && trimmed.endsWith('}')) || (trimmed.startsWith('[') && trimmed.endsWith(']'))) {
        return JSON.stringify(JSON.parse(trimmed), null, 2);
      }
      return value;
    }
    return JSON.stringify(value, null, 2);
  }
  catch {
    return String(value);
  }
}

function summarizeValue(value: unknown): string {
  if (value === undefined || value === null || value === '') {
    return '';
  }

  if (typeof value === 'string') {
    const trimmed = value.trim();
    if (!trimmed) {
      return '';
    }

    try {
      if ((trimmed.startsWith('{') && trimmed.endsWith('}')) || (trimmed.startsWith('[') && trimmed.endsWith(']'))) {
        return summarizeParsedValue(JSON.parse(trimmed));
      }
    }
    catch {
      return compactSummary(trimmed);
    }

    return compactSummary(trimmed);
  }

  return summarizeParsedValue(value);
}

function summarizeParsedValue(value: unknown): string {
  if (Array.isArray(value)) {
    if (!value.length) {
      return '返回空列表';
    }
    const firstItem = value[0];
    const firstItemSummary = firstItem && typeof firstItem === 'object'
      ? summarizeRecord(firstItem as Record<string, unknown>)
      : stringifySummaryPart(firstItem);
    return compactSummary(firstItemSummary ? `${value.length} 条记录，首条：${firstItemSummary}` : `${value.length} 条记录`);
  }

  if (value && typeof value === 'object') {
    const record = value as Record<string, unknown>;
    const data = record.data ?? record.result ?? record.output;
    if (Array.isArray(data)) {
      return data.length ? `${data.length} 条记录` : '返回空列表';
    }
    if (data && typeof data === 'object') {
      const dataRecord = data as Record<string, unknown>;
      const total = dataRecord.total ?? dataRecord.count;
      const preferredList = dataRecord.records ?? dataRecord.list ?? dataRecord.items ?? dataRecord.rows;
      const fallbackListEntry = Object.entries(dataRecord).find(([, item]) => Array.isArray(item));
      const list = preferredList ?? fallbackListEntry?.[1];
      if (Array.isArray(list)) {
        const listName = preferredList ? '' : `${fallbackListEntry?.[0] || '列表'}：`;
        return compactSummary(`${listName}${list.length} 条记录${total !== undefined ? `，总计 ${String(total)}` : ''}`);
      }
      const nestedSummary = summarizeRecord(dataRecord);
      if (nestedSummary) {
        return compactSummary(nestedSummary);
      }
    }

    const priorityKeys = ['summary', 'message', 'msg', 'content', 'remark', 'error', 'status'];
    for (const key of priorityKeys) {
      const summary = stringifySummaryPart(record[key]);
      if (summary && !isWeakSummary(summary)) {
        return compactSummary(summary);
      }
    }

    const recordSummary = summarizeRecord(record);
    if (recordSummary) {
      return compactSummary(recordSummary);
    }
  }

  return compactSummary(String(value));
}

function isWeakSummary(value: string): boolean {
  return ['success', 'ok', 'true', '成功'].includes(value.trim().toLowerCase());
}

function summarizeRecord(record: Record<string, unknown>): string {
  return Object.entries(record)
    .filter(([, value]) => value !== undefined && value !== null && value !== '')
    .slice(0, 3)
    .map(([key, value]): string => {
      const part: string = stringifySummaryPart(value);
      return part ? `${key}: ${part}` : '';
    })
    .filter(Boolean)
    .join('；');
}

function stringifySummaryPart(value: unknown): string {
  if (value === undefined || value === null || value === '') {
    return '';
  }
  if (typeof value === 'string' || typeof value === 'number' || typeof value === 'boolean') {
    return String(value);
  }
  if (Array.isArray(value)) {
    return `${value.length} 条`;
  }
  if (typeof value === 'object') {
    return summarizeRecord(value as Record<string, unknown>);
  }
  return String(value);
}

function compactSummary(value: string): string {
  const compacted = value.replace(/\s+/g, ' ').trim();
  if (compacted.length <= 120) {
    return compacted;
  }
  return `${compacted.slice(0, 117)}...`;
}

const parsedResult = computed<Record<string, any> | null>(() => {
  if (!props.toolInfo.result) {
    return null;
  }

  try {
    const raw = typeof props.toolInfo.result === 'string'
      ? props.toolInfo.result
      : JSON.stringify(props.toolInfo.result);
    const parsed = JSON.parse(raw);

    if (parsed && typeof parsed === 'object') {
      return parsed;
    }
  }
  catch {
    return null;
  }

  return null;
});

const confirmDraftInfo = computed(() => {
  if (props.toolInfo.status !== 'success') {
    return null;
  }

  const textResult = typeof props.toolInfo.result === 'string' ? props.toolInfo.result : '';
  const textDraftId = textResult.match(/草稿ID[:：]\s*([0-9a-fA-F-]{16,})/)?.[1]
    || textResult.match(/draftId["'\s:=：]+([0-9a-fA-F-]{16,})/)?.[1];

  if (textDraftId) {
    return {
      draftId: textDraftId,
      summary: textResult.includes('草稿信息') ? textResult : '',
      expiresInSeconds: null,
    };
  }

  const result = parsedResult.value;
  if (!result) {
    return null;
  }

  const data = result.data && typeof result.data === 'object' ? result.data : result;
  const draftId = data.draftId || data.draft_id;
  const needConfirm = data.needConfirm === true || data.action === 'confirm_required';

  if (!needConfirm || !draftId) {
    return null;
  }

  return {
    draftId: String(draftId),
    summary: data.summary || data.content || data.remark || '',
    expiresInSeconds: data.expiresInSeconds || data.expires_in_seconds,
  };
});

function handleConfirmDraft(event: MouseEvent) {
  event.stopPropagation();
  if (!confirmDraftInfo.value) {
    return;
  }
  emit('confirmDraft', confirmDraftInfo.value.draftId);
}
</script>

<template>
  <div class="tool-call-card" :class="{ expanded: isExpanded }">
    <!-- 卡片头部 - 始终显示 -->
    <div class="card-header" @click="isExpanded = !isExpanded">
      <div class="header-left">
        <!-- 工具图标 -->
        <div class="tool-icon">
          <el-icon :size="16">
            <Tools />
          </el-icon>
        </div>
        <div class="tool-main">
          <div class="tool-title-row">
            <!-- 工具名称 -->
            <span class="tool-name">{{ toolInfo.name || '工具调用' }}</span>
            <span class="tool-type">{{ typeText }}</span>
            <!-- 状态标签 -->
            <el-tag
              :style="{
                color: statusConfig.color,
                backgroundColor: statusConfig.bgColor,
                borderColor: statusConfig.color,
              }"
              size="small"
              class="status-tag"
            >
              <el-icon v-if="toolInfo.status === 'pending'" class="is-loading" :size="12">
                <Loading />
              </el-icon>
              <el-icon v-else :size="12">
                <component :is="statusConfig.icon" />
              </el-icon>
              <span class="status-text">{{ statusConfig.text }}</span>
            </el-tag>
          </div>
          <div class="result-summary">
            {{ resultSummary }}
          </div>
        </div>
      </div>
      <div class="header-right">
        <!-- 调用时间 -->
        <span v-if="formattedTime" class="call-time">{{ formattedTime }}</span>
        <!-- 展开/收起图标 -->
        <el-icon class="expand-icon" :class="{ rotated: isExpanded }">
          <ArrowDown />
        </el-icon>
      </div>
    </div>

    <!-- 展开内容 -->
    <el-collapse-transition>
      <div v-show="isExpanded" class="card-content">
        <div v-if="confirmDraftInfo" class="confirm-section">
          <div class="confirm-copy">
            <div class="confirm-title">
              CRM跟进记录待确认
            </div>
            <div v-if="confirmDraftInfo.summary" class="confirm-summary">
              {{ confirmDraftInfo.summary }}
            </div>
            <div v-if="confirmDraftInfo.expiresInSeconds" class="confirm-expire">
              草稿 {{ Math.floor(Number(confirmDraftInfo.expiresInSeconds) / 60) }} 分钟内有效
            </div>
          </div>
          <el-button type="primary" size="small" @click="handleConfirmDraft">
            确认入库
          </el-button>
        </div>

        <!-- 输入参数 -->
        <div v-if="formattedInput" class="result-section input-section">
          <div class="section-label">
            调用参数
          </div>
          <div class="result-content">
            <pre>{{ formattedInput }}</pre>
          </div>
        </div>

        <!-- 结果详情 -->
        <div v-if="formattedResult" class="result-section">
          <div class="section-label">
            执行结果
          </div>
          <div class="result-content">
            <pre>{{ formattedResult }}</pre>
          </div>
        </div>
        <!-- 无结果时显示 -->
        <div v-else-if="toolInfo.status === 'pending'" class="pending-hint">
          <el-icon class="is-loading">
            <Loading />
          </el-icon>
          <span>正在执行中...</span>
        </div>
        <div v-else class="no-result">
          <span>暂无返回结果</span>
        </div>
      </div>
    </el-collapse-transition>
  </div>
</template>

<style scoped lang="scss">
.tool-call-card {
  background-color: #ffffff;
  border: 1px solid transparent;
  border-radius: 8px;
  overflow: hidden;
  margin: 0;
  font-size: 13px;
  box-shadow: none;
  transition: background-color 0.2s ease, border-color 0.2s ease;

  &:hover {
    background-color: #f8fafc;
    border-color: #e2e8f0;
  }

  &.expanded {
    background-color: #f8fafc;
    border-color: #dbe4ef;
  }

  .card-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 8px 10px;
    cursor: pointer;
    user-select: none;

    .header-left {
      display: flex;
      align-items: center;
      gap: 9px;
      flex: 1;
      min-width: 0;

      .tool-icon {
        display: flex;
        align-items: center;
        justify-content: center;
        width: 26px;
        height: 26px;
        background: linear-gradient(135deg, #5f7bdc 0%, #5a47b8 100%);
        border-radius: 6px;
        color: #ffffff;
        flex-shrink: 0;
        box-shadow: 0 6px 14px rgb(90 71 184 / 22%);
      }

      .tool-name {
        font-weight: 650;
        color: #273142;
        white-space: nowrap;
        overflow: hidden;
        text-overflow: ellipsis;
      }

      .tool-type {
        padding: 2px 6px;
        border-radius: 999px;
        background-color: #eef2ff;
        color: #4f46e5;
        font-size: 11px;
        font-weight: 700;
        flex-shrink: 0;
      }

      .status-tag {
        display: flex;
        align-items: center;
        gap: 4px;
        height: 22px;
        border-radius: 6px;
        padding: 0 8px;
        font-size: 12px;
        font-weight: 650;
        flex-shrink: 0;

        .status-text {
          line-height: 1;
        }
      }

      .tool-main {
        display: flex;
        flex-direction: column;
        gap: 3px;
        flex: 1;
        min-width: 0;
      }

      .tool-title-row {
        display: flex;
        align-items: center;
        gap: 7px;
        min-width: 0;
      }

      .result-summary {
        max-width: 100%;
        overflow: hidden;
        color: #64748b;
        font-size: 12px;
        line-height: 1.35;
        text-overflow: ellipsis;
        white-space: nowrap;
      }
    }

    .header-right {
      display: flex;
      align-items: center;
      gap: 8px;
      flex-shrink: 0;

      .call-time {
        font-size: 12px;
        color: #8a93a3;
      }

      .expand-icon {
        color: #8a93a3;
        transition: transform 0.3s ease;

        &.rotated {
          transform: rotate(180deg);
        }
      }
    }
  }

  .card-content {
    padding: 0 10px 10px 45px;
    border-top: 0;

    .result-section {
      margin-top: 2px;

      &.input-section {
        margin-bottom: 10px;

        .result-content {
          background-color: #f8fafc;
          border: 1px solid #dbe4ef;

          pre {
            color: #334155;
          }
        }
      }

      .section-label {
        font-size: 12px;
        color: #64748b;
        margin-bottom: 6px;
        font-weight: 650;
      }

      .result-content {
        background-color: #0f172a;
        border-radius: 6px;
        padding: 10px 12px;
        max-height: 240px;
        overflow: auto;

        pre {
          margin: 0;
          font-family: 'Monaco', 'Menlo', 'Consolas', monospace;
          font-size: 12px;
          line-height: 1.55;
          color: #dbeafe;
          white-space: pre-wrap;
          word-break: break-word;
        }
      }
    }

    .confirm-section {
      display: flex;
      align-items: flex-start;
      justify-content: space-between;
      gap: 12px;
      padding: 10px 12px;
      margin: 2px 0 10px;
      background-color: #eef6ff;
      border: 1px solid #bfdbfe;
      border-radius: 6px;

      .confirm-copy {
        min-width: 0;
        color: #1e3a5f;
      }

      .confirm-title {
        font-weight: 700;
        margin-bottom: 4px;
      }

      .confirm-summary {
        color: #334155;
        line-height: 1.5;
        word-break: break-word;
      }

      .confirm-expire {
        margin-top: 4px;
        color: #64748b;
        font-size: 12px;
      }
    }

    .pending-hint {
      display: flex;
      align-items: center;
      justify-content: flex-start;
      gap: 8px;
      padding: 10px 0 2px;
      color: #E6A23C;

      .is-loading {
        animation: rotate 1s linear infinite;
      }
    }

    .no-result {
      padding: 10px 0 2px;
      color: #8a93a3;
      font-size: 12px;
    }
  }
}

@keyframes rotate {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}
</style>
