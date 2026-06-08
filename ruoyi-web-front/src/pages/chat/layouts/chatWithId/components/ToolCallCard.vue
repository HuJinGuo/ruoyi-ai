<script setup lang="ts">
import type { ToolCallInfo } from '@/pages/chat/layouts/chatWithId/types';

const props = defineProps<{
  toolInfo: ToolCallInfo;
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
  if (!props.toolInfo.result)
    return null;
  try {
    // 如果结果是字符串，尝试解析
    if (typeof props.toolInfo.result === 'string') {
      return props.toolInfo.result;
    }
    return JSON.stringify(props.toolInfo.result, null, 2);
  }
  catch {
    return String(props.toolInfo.result);
  }
});
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
        <!-- 工具名称 -->
        <span class="tool-name">{{ toolInfo.name || '工具调用' }}</span>
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
