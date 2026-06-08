<!-- 每个回话对应的聊天内容 -->
<script setup lang="ts">
import type { AnyObject } from 'typescript-api-pro';
import type { BubbleProps } from 'vue-element-plus-x/types/Bubble';
import type { BubbleListInstance } from 'vue-element-plus-x/types/BubbleList';
import type { ThinkingStatus } from 'vue-element-plus-x/types/Thinking';
import type { ToolCallInfo } from './types';
import { useHookFetch } from 'hook-fetch/vue';
import { nextTick } from 'vue';
import { useRoute } from 'vue-router';
import { send, sendAgent } from '@/api';
import ChatSender from '@/components/ChatSender/index.vue';
import { useChatStore } from '@/stores/modules/chat';
import { useModelStore } from '@/stores/modules/model';
import { useSessionStore } from '@/stores/modules/session';
import { useUserStore } from '@/stores/modules/user';
import { codeXRender } from '@/utils/markdownRenderers';
import ToolCallCard from './components/ToolCallCard.vue';

type MessageItem = BubbleProps & {
  key: number;
  role: 'ai' | 'user' | 'system';
  avatar: string;
  thinkingStatus?: ThinkingStatus;
  thinlCollapse?: boolean;
  reasoning_content?: string;
  toolCalls?: ToolCallInfo[];
  toolCallsCollapsed?: boolean;
  class?: string;
};

const route = useRoute();
const chatStore = useChatStore();
const modelStore = useModelStore();
const sessionStore = useSessionStore();
const userStore = useUserStore();

// 用户头像
const avatar = computed(() => {
  const userInfo = userStore.userInfo;
  return userInfo?.avatar || 'https://avatars.githubusercontent.com/u/32251822?s=96&v=4';
});

const inputValue = ref('');
const chatSenderRef = ref<InstanceType<typeof ChatSender> | null>(null);
const bubbleItems = ref<MessageItem[]>([]);
const bubbleListRef = ref<BubbleListInstance | null>(null);
const executionEvents = ref<ToolCallInfo[]>([]);
const activeAgentRunId = ref('');

// 工具调用事件计数器（用于生成唯一 key）
let toolCallKeyCounter = 0;

const copyIconMap = ref<Record<number, string>>({}); // 记录每条消息的复制按钮图标
const editingMessageKeys = ref<number[]>([]); // 跟踪多个编辑中的消息
const editedContents = ref<Record<number, string>>({}); // 存储每条消息的临时编辑内容

const {
  stream,
  loading: isLoading,
  cancel,
} = useHookFetch({
  request: (data: any) => data?.assistantCode
    ? sendAgent(data.assistantCode, data)
    : send(data),
  onError: (err) => {
    console.warn('测试错误拦截', err);
  },
});

// 从 localStorage 恢复推理状态
onMounted(() => {
  bubbleItems.value.forEach((item) => {
    copyIconMap.value[item.key] = 'CopyDocument';
  });
  const enableThinking = localStorage.getItem('enableThinking');
  if (enableThinking === 'true' && chatSenderRef.value) {
    chatSenderRef.value.isReasoningEnabled = true;
    localStorage.removeItem('enableThinking');
  }
});

// 记录进入思考中
let isThinking = false;

watch(
  () => route.params?.id,
  async (_id_) => {
    if (_id_) {
      toolCallKeyCounter = 0;

      if (_id_ !== 'not_login') {
        // 判断的当前会话id是否有聊天记录，有缓存则直接赋值展示
        if (chatStore.chatMap[`${_id_}`] && chatStore.chatMap[`${_id_}`].length) {
          bubbleItems.value = chatStore.chatMap[`${_id_}`] as MessageItem[];
          // 滚动到底部
          setTimeout(() => {
            bubbleListRef.value?.scrollToBottom();
          }, 350);
          return;
        }

        // 无缓存则请求聊天记录
        await chatStore.requestChatList(`${_id_}`);
        // 请求聊天记录后，赋值回显，并滚动到底部
        bubbleItems.value = chatStore.chatMap[`${_id_}`] as MessageItem[];

        // 滚动到底部
        setTimeout(() => {
          bubbleListRef.value?.scrollToBottom();
        }, 350);
      }

      // 如果本地有发送内容 ，则直接发送
      const v = localStorage.getItem('chatContent');
      if (v) {
        // 发送消息
        setTimeout(() => {
          startSSE(v);
        }, 350);

        localStorage.removeItem('chatContent');
      }
    }
  },
  { immediate: true, deep: true },
);

// 封装错误处理逻辑
function handleError(err: any) {
  console.error('Fetch error:', err);
}

async function startSSE(chatContent: string) {
  try {
    toolCallKeyCounter = 0;
    executionEvents.value = [];
    activeAgentRunId.value = '';

    // 添加用户输入的消息
    inputValue.value = '';
    addMessage(chatContent, true);
    addMessage('', false);

    // 这里有必要调用一下 BubbleList 组件的滚动到底部 手动触发 自动滚动
    bubbleListRef.value?.scrollToBottom();

    // 获取最后一条用户消息（后端做了长期记忆缓存，只需发送最新的用户消息）
    const lastUserMessage = bubbleItems.value.filter((item: any) => item.role === 'user').pop();

    // 标记是否收到第一个有效数据 chunk（用于清除 loading 状态）
    let hasReceivedFirstContent = false;

    const useAgentMode = chatSenderRef.value?.isReasoningEnabled || false;

    for await (const chunk of stream({
      assistantCode: useAgentMode ? 'internal-unified' : undefined,
      model: modelStore.currentModelInfo.modelName ?? '',
      content: lastUserMessage?.content ?? '',
      sessionId: route.params?.id !== 'not_login' ? String(route.params?.id) : undefined,
      enableThinking: !useAgentMode && (chatSenderRef.value?.isReasoningEnabled || false),
      knowledgeId: chatStore.knowledgeId || undefined,
    })) {
      // 处理数据块 - chunk.result 可能是字符串或对象
      // 返回 true 表示流结束
      const isStreamEnd = handleDataChunk(chunk.result as AnyObject | string);

      // 在收到第一个有效数据后清除 loading 状态（跳过连接状态事件）
      if (!hasReceivedFirstContent && chunk.result !== ':connected' && chunk.result !== ':disconnected' && !isStreamEnd) {
        const lastMessage = bubbleItems.value[bubbleItems.value.length - 1];
        if (lastMessage) {
          lastMessage.loading = false;
          bubbleItems.value = [...bubbleItems.value];
        }
        hasReceivedFirstContent = true;
      }

      // 等待 Vue 更新 DOM，实现真正的流式渲染
      await nextTick();

      if (isStreamEnd) {
        // 收到 done 后不主动 break，继续把浏览器/TransformStream 中已排队的事件读完。
        // 后端会随后关闭 SSE，for-await 会自然结束；主动 break 容易丢掉同批次里尚未渲染的尾部内容。
        console.log('[SSE] 收到结束事件，等待连接自然关闭');
      }
    }
  }
  catch (err) {
    handleError(err);
    // 出错时也要清除 loading 状态
    if (bubbleItems.value.length) {
      const lastMessage = bubbleItems.value[bubbleItems.value.length - 1];
      lastMessage.loading = false;
      bubbleItems.value = [...bubbleItems.value];
    }
  }
  finally {
    // 停止打字器状态
    if (bubbleItems.value.length) {
      const lastMessage = bubbleItems.value[bubbleItems.value.length - 1];
      lastMessage.typing = false;
      // 无条件重置 loading（停止打字动画）
      lastMessage.loading = false;
      // 重置思考状态：如果还在思考中，标记为已完成
      if (lastMessage.thinkingStatus === 'thinking') {
        lastMessage.thinkingStatus = 'end';
      }
      // 重置isThinking标志
      isThinking = false;
      bubbleItems.value = [...bubbleItems.value];
    }
  }
}

// 封装数据处理逻辑
function handleDataChunk(chunk: AnyObject | string): boolean {
  console.log('[SSE] 收到 chunk:', chunk, 'type:', typeof chunk);

  try {
    if (typeof chunk === 'string') {
      if (chunk === ':connected' || chunk === ':disconnected' || chunk.startsWith(':')) {
        console.log('[SSE] 连接状态:', chunk);
        return false;
      }

      return parseSseStringEvents(chunk).reduce(
        (shouldEnd, event) => handleSseEvent(event.data, event.eventType) || shouldEnd,
        false,
      );
    }
    else if (typeof chunk === 'object' && chunk !== null) {
      const eventType = chunk.event || '';

      if (eventType || chunk.done === true) {
        return handleSseEvent(chunk, eventType);
      }

      const reasoningChunk = chunk?.choices?.[0]?.delta?.reasoning_content;
      if (reasoningChunk) {
        appendReasoningContent(reasoningChunk);
      }

      const parsedChunk = chunk?.choices?.[0]?.delta?.content;
      if (parsedChunk) {
        handleContentChunk(parsedChunk);
      }

      const directContent = chunk?.content;
      if (directContent) {
        handleContentChunk(directContent);
      }

      return Boolean(chunk?.choices?.[0]?.finish_reason);
    }
  }
  catch (err) {
    console.error('解析数据时出错:', err);
  }

  return false;
}

function parseSseStringEvents(chunk: string) {
  return chunk
    .split(/\n\n+/)
    .map(block => block.trim())
    .filter(Boolean)
    .map((block) => {
      let eventType = '';
      const dataLines: string[] = [];

      for (const line of block.split('\n')) {
        if (line.startsWith('event:')) {
          eventType = line.substring(6).trim();
        }
        else if (line.startsWith('data:')) {
          dataLines.push(line.substring(5).trim());
        }
      }

      const dataText = dataLines.join('\n');
      if (!dataText) {
        return { eventType, data: { event: eventType } };
      }

      try {
        return { eventType, data: JSON.parse(dataText) as AnyObject };
      }
      catch {
        console.warn('[SSE] JSON 解析失败:', dataText);
        return { eventType, data: { event: eventType, content: dataText } };
      }
    });
}

function handleSseEvent(dataObj: AnyObject, rawEventType = ''): boolean {
  const eventType = rawEventType || dataObj.event || '';

  if (eventType.startsWith('agent_')) {
    handleAgentTraceEvent(eventType, dataObj);
  }
  else if (eventType === 'mcp' || eventType === 'mcp_tool') {
    handleMcpEvent(dataObj);
  }
  else if (eventType === 'content' || eventType === 'message' || dataObj.content) {
    const content = dataObj.content || '';
    if (content) {
      handleContentChunk(content);
    }
  }

  const reasoningContent = dataObj.reasoning_content || dataObj.reasoningContent || '';
  if (reasoningContent) {
    appendReasoningContent(reasoningContent);
  }

  if (eventType === 'error' && dataObj.error) {
    ElMessage.error(dataObj.error);
  }

  if (eventType === 'done' || dataObj.done === true) {
    console.log('[SSE] 流结束');
    return true;
  }

  return false;
}

function handleAgentTraceEvent(eventType: string, dataObj: AnyObject) {
  const payload = (dataObj.payload || dataObj.content || dataObj) as AnyObject;

  if (eventType === 'agent_run_start' && payload.runId) {
    activeAgentRunId.value = payload.runId;
    executionEvents.value = [];
  }
  else if (payload.runId && activeAgentRunId.value && payload.runId !== activeAgentRunId.value) {
    return;
  }

  const status = normalizeTraceStatus(payload.status, eventType);
  const name = payload.name || payload.title || eventType;
  const stepId = payload.stepId || payload.runId || `${eventType}-${executionEvents.value.length}`;

  const traceInfo: ToolCallInfo = {
    key: ++toolCallKeyCounter,
    name,
    status,
    result: formatTraceResult(payload, eventType),
    timestamp: payload.timestamp || Date.now(),
    type: payload.type || 'AGENT',
    runId: payload.runId,
    stepId,
    input: payload.input,
  };

  const existingIndex = executionEvents.value.findIndex(item => item.stepId === stepId && item.name === name);
  if (existingIndex >= 0) {
    executionEvents.value[existingIndex] = {
      ...executionEvents.value[existingIndex],
      ...traceInfo,
    };
  }
  else {
    executionEvents.value = [...executionEvents.value, traceInfo];
  }

  const assistantMessage = getCurrentAssistantMessage();
  if (assistantMessage) {
    bubbleItems.value = [...bubbleItems.value];
  }
}

function normalizeTraceStatus(status: unknown, eventType: string): ToolCallInfo['status'] {
  if (eventType.endsWith('_start') || status === 'running') {
    return 'pending';
  }
  if (status === 'error' || status === 'failed') {
    return 'error';
  }
  return 'success';
}

function formatTraceResult(payload: AnyObject, eventType: string) {
  if (payload.result !== undefined) {
    return typeof payload.result === 'string' ? payload.result : JSON.stringify(payload.result, null, 2);
  }
  return JSON.stringify({ event: eventType, ...payload }, null, 2);
}

function appendReasoningContent(reasoningContent: string) {
  const lastMessage = bubbleItems.value[bubbleItems.value.length - 1];
  if (lastMessage) {
    lastMessage.thinkingStatus = 'thinking';
    lastMessage.loading = true;
    lastMessage.thinlCollapse = true;
    lastMessage.reasoning_content += reasoningContent;
    bubbleItems.value = [...bubbleItems.value];
  }
}

function handleMcpEvent(dataObj: AnyObject) {
  console.log('[SSE] MCP 事件:', dataObj);

  try {
    const content = typeof dataObj.content === 'string'
      ? JSON.parse(dataObj.content)
      : dataObj.content;

    if (content?.runId && activeAgentRunId.value && content.runId !== activeAgentRunId.value) {
      return;
    }

    const toolName = content.name || content.toolName || 'Unknown Tool';
    const toolStatus = content.status || 'pending';
    const toolResult = content.result || null;
    const toolStepId = content.stepId || content.id || toolName;
    const normalizedStatus = normalizeTraceStatus(toolStatus, toolStatus === 'pending' ? 'tool_call_start' : 'tool_call_done');
    const traceInfo: ToolCallInfo = {
      key: ++toolCallKeyCounter,
      name: toolName,
      status: normalizedStatus,
      result: toolResult,
      timestamp: Date.now(),
      type: 'TOOL',
      runId: content.runId,
      stepId: toolStepId,
      input: content.input,
    };

    const index = executionEvents.value.findIndex(
      t => t.stepId === toolStepId || (t.name === toolName && t.status === 'pending'),
    );

    if (index >= 0) {
      const updatedEvents = [...executionEvents.value];
      updatedEvents[index] = {
        ...updatedEvents[index],
        ...traceInfo,
        key: updatedEvents[index].key,
      };
      executionEvents.value = updatedEvents;
    }
    else {
      executionEvents.value = [...executionEvents.value, traceInfo];
    }

    bubbleItems.value = [...bubbleItems.value];
    console.log('[SSE] 执行上下文列表:', executionEvents.value);
  }
  catch (err) {
    console.error('[SSE] MCP 事件解析失败:', err);
  }
}

function getCurrentAssistantMessage() {
  for (let i = bubbleItems.value.length - 1; i >= 0; i--) {
    const item = bubbleItems.value[i];
    if (item.role === 'system') {
      return item;
    }
  }
  return null;
}

function handleContentChunk(content: string) {
  const lastIndex = bubbleItems.value.length - 1;
  const lastMessage = bubbleItems.value[lastIndex];
  if (!lastMessage) {
    return;
  }

  let currentText = content;

  if (!isThinking && currentText.includes('<think')) {
    const thinkIdx = currentText.indexOf('<think');
    if (thinkIdx > 0) {
      const beforeThink = currentText.substring(0, thinkIdx);
      lastMessage.content += beforeThink;
    }
    currentText = currentText.substring(thinkIdx + 7);
    isThinking = true;
    lastMessage.thinkingStatus = 'thinking';
    lastMessage.loading = true;
    lastMessage.thinlCollapse = true;
  }

  if (isThinking && currentText.includes('</think')) {
    const thinkEndIdx = currentText.indexOf('</think');
    if (thinkEndIdx > 0) {
      const thinkContent = currentText.substring(0, thinkEndIdx);
      lastMessage.reasoning_content += thinkContent;
    }
    currentText = currentText.substring(thinkEndIdx + 8);
    isThinking = false;
    lastMessage.thinkingStatus = 'end';
    lastMessage.loading = false;
  }

  if (currentText) {
    if (isThinking) {
      lastMessage.reasoning_content += currentText;
    }
    else {
      lastMessage.content += currentText;
    }
  }

  bubbleItems.value = [...bubbleItems.value];
  bubbleListRef.value?.scrollToBottom();
}

async function cancelSSE() {
  cancel();
  if (bubbleItems.value.length) {
    bubbleItems.value[bubbleItems.value.length - 1].typing = false;
  }
}

function copyToClipboard(text: string, key: number) {
  navigator.clipboard
    .writeText(text)
    .then(() => {
      copyIconMap.value[key] = 'Check';
      setTimeout(() => {
        copyIconMap.value[key] = 'CopyDocument';
      }, 2000);
    })
    .catch((err) => {
      console.error('复制失败:', err);
      ElMessage.error('复制失败，请手动复制');
    });
}

function addMessage(message: string, isUser: boolean) {
  const i = bubbleItems.value.length;
  const obj: MessageItem = {
    key: i,
    avatar: isUser
      ? avatar.value
      : 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png',
    avatarSize: '32px',
    role: isUser ? 'user' : 'system',
    placement: isUser ? 'end' : 'start',
    isMarkdown: !isUser,
    loading: !isUser,
    content: message || '',
    reasoning_content: '',
    toolCalls: [],
    toolCallsCollapsed: false,
    thinkingStatus: 'start',
    thinlCollapse: false,
    noStyle: !isUser,
  };
  bubbleItems.value.push(obj);
}

function handleChange(_payload: { value: boolean; status: ThinkingStatus }) {}

function startEditing(item: MessageItem) {
  if (!editingMessageKeys.value.includes(item.key)) {
    editingMessageKeys.value.push(item.key);
    editedContents.value[item.key] = item.content || '';
  }
  item.noStyle = true;
  item.class = 'editing-bubble';
}

function cancelEditingByKey(key: number) {
  const item = bubbleItems.value.find(i => i.key === key);
  if (item) {
    item.noStyle = false;
    item.class = '';
  }
  editingMessageKeys.value = editingMessageKeys.value.filter(k => k !== key);
  delete editedContents.value[key];
}

function sendMessageByKey(key: number) {
  const newContent = editedContents.value[key];
  if (newContent) {
    startSSE(newContent);
    cancelEditingByKey(key);
  }
}

function handleCreateNewChat() {
  sessionStore.createSessionBtn();
}
</script>

<template>
  <div class="chat-with-id-container">
    <div class="chat-warp">
      <BubbleList ref="bubbleListRef" :list="bubbleItems" max-height="calc(100vh - 240px)">
        <template #header="{ item }">
          <Thinking
            v-if="item.reasoning_content"
            v-model="item.thinlCollapse"
            :content="item.reasoning_content"
            :status="item.thinkingStatus"
            class="thinking-chain-warp"
            @change="handleChange"
          />
        </template>

        <template #content="{ item }">
          <div v-if="item.role === 'system'" class="assistant-answer-card">
            <XMarkdown
              v-if="item.content"
              :markdown="item.content"
              :code-x-render="codeXRender"
              class="markdown-body"
              :themes="{ light: 'github-light', dark: 'github-dark' }"
              default-theme-mode="dark"
            />
          </div>

          <div v-if="item.content && item.role === 'user'" class="userContent">
            <div class="user-bubble" :class="{ editing: editingMessageKeys.includes(item.key) }">
              <template v-if="!editingMessageKeys.includes(item.key)">
                <div class="user-content">
                  {{ item.content }}
                </div>
              </template>

              <template v-else>
                <div class="edit-card">
                  <el-input
                    v-model="editedContents[item.key]"
                    type="textarea"
                    autosize
                    class="edit-input"
                  />
                  <div class="edit-actions">
                    <el-button size="small" @click="cancelEditingByKey(item.key)">
                      取消
                    </el-button>
                    <el-button type="primary" size="small" @click="sendMessageByKey(item.key)">
                      发送
                    </el-button>
                  </div>
                </div>
              </template>
            </div>

            <div v-if="!editingMessageKeys.includes(item.key)" class="copy-button-container">
              <el-tooltip content="复制" placement="bottom">
                <el-button
                  class="copy-btn"
                  :icon="copyIconMap[item.key] || 'CopyDocument'"
                  size="small"
                  @click="copyToClipboard(item.content, item.key)"
                />
              </el-tooltip>
              <el-tooltip content="编辑" placement="bottom">
                <el-button class="copy-btn" icon="Edit" size="small" @click="startEditing(item)" />
              </el-tooltip>
            </div>
          </div>
        </template>
      </BubbleList>

      <div class="sender-wrapper">
        <!-- 新对话按钮 -->
        <div class="sender-actions-row">
          <div class="new-chat-btn" @click="handleCreateNewChat">
            <el-icon class="btn-icon">
              <Plus />
            </el-icon>
            <span class="btn-text">新对话</span>
          </div>
        </div>

        <ChatSender
          ref="chatSenderRef"
          v-model="inputValue"
          :loading="isLoading"
          @submit="startSSE"
          @cancel="cancelSSE"
        />
      </div>
    </div>

    <aside class="agent-execution-panel">
      <div class="execution-header">
        <div>
          <div class="execution-title">
            执行上下文
          </div>
          <div class="execution-subtitle">
            Agent、工具与计划
          </div>
        </div>
        <el-tag size="small" type="info">
          {{ executionEvents.length }} steps
        </el-tag>
      </div>

      <div class="execution-toolbar">
        <span>{{ executionEvents.filter(item => item.status === 'success').length }} done</span>
        <span>{{ executionEvents.filter(item => item.status === 'pending').length }} running</span>
        <button type="button" @click="executionEvents = []">
          清空
        </button>
      </div>

      <div class="execution-hint">
        工具详情只在这里滚动展示，不占用对话正文空间。
      </div>

      <div class="execution-list">
        <ToolCallCard
          v-for="trace in executionEvents"
          :key="trace.key"
          :tool-info="trace"
        />
        <div v-if="!executionEvents.length" class="execution-empty">
          开启内部统一 Agent 后，这里会显示协作步骤。
        </div>
      </div>
    </aside>
  </div>
</template>

<style scoped lang="scss">
.user-bubble.editing {
  background: transparent !important;
  padding: 0;
}

:deep(.editing-bubble.el-bubble) {
  display: flex !important;
  width: 100% !important;
  justify-content: flex-start !important;
}

:deep(.editing-bubble .el-bubble__content) {
  flex: 1 !important;
  max-width: none !important;
  width: 100% !important;
}

.edit-card {
  width: 500px;
  box-sizing: border-box;
  border: 1px solid #dcdfe6;
  border-radius: 16px;
  padding: 12px;
  background: #ffffff;
  transition: all 0.2s ease;
}

.edit-input :deep(.el-textarea__inner) {
  border: none !important;
  box-shadow: none !important;
  background: transparent !important;
  resize: none;
  padding: 0;
  font-size: 14px;
}

.edit-actions {
  margin-top: 12px;
  display: flex;
  justify-content: flex-end;
  gap: 4px;
}

.copy-button-container {
  position: absolute;
  bottom: -28px;
  right: -10px;
  transform: translateY(10px);
  transition: all 0.3s ease;
  pointer-events: none;
  display: flex;
  justify-content: flex-end;

  .copy-btn {
    width: 24px;
    height: 24px;
    padding: 0;
    font-size: 16px;
    cursor: pointer;
    pointer-events: auto;
    border: none !important;
    color: #91949a;
    :deep(svg) {
      stroke-width: 3 !important;
    }

    &:hover {
      border-radius: 50%;
      transition: background-color 0.2s;
      background-color: #f1efef;
    }
  }
}

.chat-with-id-container {
  position: relative;
  display: flex;
  flex-direction: row;
  align-items: stretch;
  justify-content: center;
  gap: 28px;
  width: 100%;
  max-width: 1280px;
  height: 100%;
  padding: 0 24px;
  box-sizing: border-box;

  .chat-warp {
    display: flex;
    flex-direction: column;
    justify-content: space-between;
    width: 100%;
    max-width: 800px;
    height: calc(100vh - 60px);

    .thinking-chain-warp {
      margin-bottom: 12px;
    }

    .sender-wrapper {
      display: flex;
      flex-direction: column;
      gap: 10px;
      width: 100%;
      margin-bottom: 22px;

      .sender-actions-row {
        display: flex;
        justify-content: flex-start;
        width: 100%;
      }

      .new-chat-btn {
        display: inline-flex;
        gap: 6px;
        align-items: center;
        padding: 6px 12px;
        cursor: pointer;
        user-select: none;
        background-color: #ffffff;
        border: 1px solid rgb(0 0 0 / 10%);
        border-radius: 16px;
        box-shadow: 0 1px 2px rgb(0 0 0 / 5%);
        transition: all 0.2s ease;

        &:hover {
          background-color: rgb(0 87 255 / 4%);
          border-color: rgb(0 87 255 / 20%);
          box-shadow: 0 2px 4px rgb(0 87 255 / 10%);
          .btn-icon {
            color: #0057ff;
          }
        }

        .btn-icon {
          width: 16px;
          height: 16px;
          font-size: 16px;
          color: rgb(0 0 0 / 65%);
          transition: color 0.2s ease;
        }

        .btn-text {
          font-size: 13px;
          font-weight: 500;
          color: rgb(0 0 0 / 85%);
        }
      }
    }

    @media (max-width: 640px) {
      .sender-wrapper {
        margin-bottom: 14px;
      }
    }
  }

  :deep() {
    .el-bubble-list {
      padding-top: 24px;
    }
    .el-bubble {
      padding: 0 12px;
      padding-bottom: 24px;
    }
    .el-typewriter {
      overflow: hidden;
      border-radius: 12px;
    }
    .user-content {
      white-space: pre-wrap;
    }
    .markdown-body {
      background-color: transparent;
      width: auto;
      max-width: none;
      overflow: visible;
    }
    .markdown-elxLanguage-header-div {
      top: -25px !important;
    }
    .elx-xmarkdown-container {
      padding: 8px 4px;
      width: 100%;
      overflow: visible;
    }
  }
}

.agent-execution-panel {
  width: 360px;
  height: calc(100vh - 60px);
  border-left: 1px solid #edf0f5;
  background: #ffffff;
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
}

.execution-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 22px 18px 14px;
  border-bottom: 1px solid #edf0f5;
}

.execution-title {
  font-size: 16px;
  font-weight: 700;
  color: #1f2937;
}

.execution-subtitle {
  margin-top: 4px;
  font-size: 12px;
  color: #8a94a6;
}

.execution-toolbar {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 18px;
  color: #6b7280;
  font-size: 12px;

  span {
    padding: 3px 8px;
    border-radius: 7px;
    background: #f3f5f8;
    font-weight: 650;
  }

  button {
    border: none;
    background: transparent;
    color: #5b6472;
    cursor: pointer;
    font-size: 12px;
    padding: 3px 4px;
  }
}

.execution-hint {
  margin: 0 18px 10px;
  padding: 10px 12px;
  border: 1px dashed #d9dee8;
  border-radius: 8px;
  color: #8a94a6;
  font-size: 12px;
  line-height: 1.5;
}

.execution-list {
  flex: 1;
  overflow-y: auto;
  padding: 0 18px 18px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.execution-empty {
  color: #9aa3b2;
  font-size: 13px;
  padding: 20px 4px;
}

@media (max-width: 1180px) {
  .chat-with-id-container {
    padding: 0 16px;
  }

  .agent-execution-panel {
    display: none;
  }
}

.assistant-answer-card {
  width: min(100%, 680px);
  padding: 12px 14px;
  background: linear-gradient(180deg, rgb(248 250 252 / 92%), rgb(255 255 255 / 96%));
  border: 1px solid rgb(226 232 240 / 90%);
  border-radius: 8px;
  box-shadow: 0 8px 22px rgb(15 23 42 / 6%);

  :deep(.elx-xmarkdown-container) {
    padding: 10px 0 0;
  }
}

</style>
