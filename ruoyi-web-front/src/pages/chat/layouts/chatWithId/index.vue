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
  agentProgress?: string;
  agentMode?: boolean;
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
const hasAssistantContent = ref(false);

// 工具调用事件计数器（用于生成唯一 key）
let toolCallKeyCounter = 0;
let traceOrderCounter = 0;

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
let pendingSseText = '';

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
  let endedWithError = false;
  try {
    toolCallKeyCounter = 0;
    traceOrderCounter = 0;
    pendingSseText = '';
    executionEvents.value = [];
    activeAgentRunId.value = '';
    hasAssistantContent.value = false;
    const useAgentMode = shouldUseInternalAgent(chatContent);

    // 添加用户输入的消息
    inputValue.value = '';
    addMessage(chatContent, true);
    addMessage('', false, { agentMode: useAgentMode });

    // 这里有必要调用一下 BubbleList 组件的滚动到底部 手动触发 自动滚动
    bubbleListRef.value?.scrollToBottom();

    // 获取最后一条用户消息（后端做了长期记忆缓存，只需发送最新的用户消息）
    const lastUserMessage = bubbleItems.value.filter((item: any) => item.role === 'user').pop();

    // 标记是否收到第一个有效数据 chunk（用于清除 loading 状态）
    let hasReceivedFirstContent = false;

    for await (const chunk of stream({
      assistantCode: useAgentMode ? 'internal-unified' : undefined,
      model: modelStore.currentModelInfo.modelName ?? '',
      content: lastUserMessage?.content ?? '',
      sessionId: route.params?.id !== 'not_login' ? String(route.params?.id) : undefined,
      enableThinking: chatSenderRef.value?.isReasoningEnabled || useAgentMode,
      knowledgeId: chatStore.knowledgeId || undefined,
    })) {
      // 处理数据块 - chunk.result 可能是字符串或对象
      // 返回 true 表示流结束
      const isStreamEnd = await handleDataChunk(chunk.result as AnyObject | string);

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

      // 收到 done 后不主动 break，继续把浏览器/TransformStream 中已排队的事件读完。
      // 后端会随后关闭 SSE，for-await 会自然结束；主动 break 容易丢掉同批次里尚未渲染的尾部内容。
    }

    await flushPendingSseEvents();
  }
  catch (err) {
    endedWithError = true;
    handleError(err);
    // 出错时也要清除 loading 状态
    if (bubbleItems.value.length) {
      const lastMessage = bubbleItems.value[bubbleItems.value.length - 1];
      lastMessage.loading = false;
      bubbleItems.value = [...bubbleItems.value];
    }
  }
  finally {
    finalizePendingTraceEvents(endedWithError ? 'error' : 'success');
    setAgentProgress('');
    // 停止打字器状态
    if (bubbleItems.value.length) {
      const lastMessage = bubbleItems.value[bubbleItems.value.length - 1];
      lastMessage.typing = false;
      // 无条件重置 loading（停止打字动画）
      lastMessage.loading = false;
      lastMessage.agentProgress = '';
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

function handleConfirmDraft(draftId: string) {
  if (isLoading.value) {
    ElMessage.warning('当前对话还在执行中，请稍后再确认');
    return;
  }

  startSSE(`确认新增这条CRM跟进记录，draftId: ${draftId}`);
}

// 封装数据处理逻辑
async function handleDataChunk(chunk: AnyObject | string): Promise<boolean> {
  try {
    if (typeof chunk === 'string') {
      const trimmedChunk = chunk.trim();
      if (trimmedChunk === ':connected' || trimmedChunk === ':disconnected' || (trimmedChunk.startsWith(':') && !trimmedChunk.includes('\n'))) {
        return false;
      }

      let shouldEnd = false;
      for (const event of parseSseStringEvents(chunk)) {
        shouldEnd = handleSseEvent(event.data, event.eventType) || shouldEnd;
        await waitForSseRenderFrame(event.eventType || event.data?.event || '');
      }
      return shouldEnd;
    }
    else if (typeof chunk === 'object' && chunk !== null) {
      const eventType = chunk.event || '';

      if (eventType || chunk.done === true) {
        const shouldEnd = handleSseEvent(chunk, eventType);
        await waitForSseRenderFrame(eventType);
        return shouldEnd;
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

async function flushPendingSseEvents(): Promise<boolean> {
  let shouldEnd = false;
  for (const event of parseSseStringEvents('', true)) {
    shouldEnd = handleSseEvent(event.data, event.eventType) || shouldEnd;
    await waitForSseRenderFrame(event.eventType || event.data?.event || '');
  }
  return shouldEnd;
}

async function waitForSseRenderFrame(eventType: string) {
  await nextTick();
  if (eventType.startsWith('agent_')) {
    await new Promise(resolve => setTimeout(resolve, 48));
  }
}

function parseSseStringEvents(chunk: string, flush = false) {
  pendingSseText += chunk.replace(/\r\n/g, '\n');
  const events: Array<{ eventType: string; data: AnyObject }> = [];

  while (pendingSseText) {
    const separator = pendingSseText.match(/\n{2,}/);
    if (!separator || separator.index === undefined) {
      break;
    }

    const block = pendingSseText.slice(0, separator.index);
    pendingSseText = pendingSseText.slice(separator.index + separator[0].length);
    const parsedEvent = parseSseBlock(block, false);
    if (parsedEvent) {
      events.push(parsedEvent);
    }
  }

  if (pendingSseText.trim()) {
    const parsedEvent = parseSseBlock(pendingSseText, !flush);
    if (parsedEvent) {
      events.push(parsedEvent);
      pendingSseText = '';
    }
    else if (flush) {
      pendingSseText = '';
    }
  }

  return events;
}

function parseSseBlock(block: string, waitForCompleteJson: boolean) {
  const normalizedBlock = block.trim();
  if (!normalizedBlock) {
    return null;
  }

  let eventType = '';
  let hasSseField = false;
  const dataLines: string[] = [];

  for (const line of normalizedBlock.split('\n')) {
    if (line.startsWith('event:')) {
      eventType = line.substring(6).trim();
      hasSseField = true;
    }
    else if (line.startsWith('data:')) {
      dataLines.push(line.substring(5).trim());
      hasSseField = true;
    }
    else if (line.startsWith(':')) {
      hasSseField = true;
    }
  }

  const dataText = dataLines.join('\n');
  if (!dataText) {
    if (hasSseField) {
      return { eventType, data: { event: eventType } };
    }
    return { eventType: 'content', data: { event: 'content', content: normalizedBlock } };
  }

  try {
    return { eventType, data: JSON.parse(dataText) as AnyObject };
  }
  catch {
    if (waitForCompleteJson && isLikelyJsonStart(dataText)) {
      return null;
    }
    console.warn('[SSE] JSON 解析失败:', dataText);
    return { eventType: eventType || 'content', data: { event: eventType || 'content', content: dataText } };
  }
}

function isLikelyJsonStart(value: string) {
  const text = value.trim();
  return text.startsWith('{') || text.startsWith('[') || text.startsWith('"');
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
    finalizePendingTraceEvents('error');
    setAgentProgress('');
    ElMessage.error(dataObj.error);
  }

  if (eventType === 'done' || dataObj.done === true) {
    return true;
  }

  return false;
}

function handleAgentTraceEvent(eventType: string, dataObj: AnyObject) {
  const payload = (dataObj.payload || dataObj.content || dataObj) as AnyObject;

  if (eventType === 'agent_run_start' && payload.runId) {
    const shouldResetTrace = Boolean(activeAgentRunId.value && payload.runId !== activeAgentRunId.value);
    activeAgentRunId.value = payload.runId;
    if (shouldResetTrace) {
      executionEvents.value = [];
      traceOrderCounter = 0;
    }
    setAgentProgress('正在分析问题并准备业务查询链路');
    syncCurrentAssistantToolCalls();
    bubbleItems.value = [...bubbleItems.value];
    return;
  }
  else if (payload.runId && activeAgentRunId.value && payload.runId !== activeAgentRunId.value) {
    return;
  }

  if (eventType === 'agent_run_done') {
    finalizePendingTraceEvents(normalizeTraceStatus(payload.status, eventType));
    setAgentProgress('');
    bubbleItems.value = [...bubbleItems.value];
    return;
  }

  const status = normalizeTraceStatus(payload.status, eventType);
  const stepId = resolveTraceStepId(eventType, payload);
  const existingIndex = executionEvents.value.findIndex(item => item.stepId === stepId);
  const existingEvent = existingIndex >= 0 ? executionEvents.value[existingIndex] : null;
  const name = resolveTraceName(eventType, payload, existingEvent);

  const traceInfo: ToolCallInfo = {
    key: ++toolCallKeyCounter,
    name,
    status,
    result: formatTraceResult(payload, eventType),
    timestamp: payload.timestamp || Date.now(),
    type: eventType === 'agent_step_log' ? 'LOG' : payload.type || 'AGENT',
    runId: payload.runId,
    stepId,
    input: extractTraceInput(payload),
    order: existingEvent?.order ?? ++traceOrderCounter,
  };

  if (existingIndex >= 0) {
    executionEvents.value[existingIndex] = {
      ...executionEvents.value[existingIndex],
      ...traceInfo,
      key: executionEvents.value[existingIndex].key,
    };
  }
  else {
    executionEvents.value = [...executionEvents.value, traceInfo];
  }

  updateAgentProgress(traceInfo, eventType);

  const assistantMessage = getCurrentAssistantMessage();
  if (assistantMessage) {
    syncCurrentAssistantToolCalls();
    bubbleItems.value = [...bubbleItems.value];
  }
}

function resolveTraceStepId(eventType: string, payload: AnyObject) {
  if (eventType === 'agent_plan' && payload.runId) {
    return `plan:${payload.runId}`;
  }
  if (eventType === 'agent_step_log') {
    return `log:${payload.stepId || payload.name || payload.runId}:${payload.timestamp || Date.now()}:${traceOrderCounter + 1}`;
  }
  if ((eventType.startsWith('agent_tool') || payload.type === 'TOOL') && (payload.stepId || payload.id || payload.name)) {
    return payload.stepId || `tool:${payload.id || payload.name}`;
  }
  return payload.stepId || payload.id || payload.runId || `${eventType}-${executionEvents.value.length}`;
}

function resolveTraceName(eventType: string, payload: AnyObject, existingEvent: ToolCallInfo | null) {
  if (eventType === 'agent_plan') {
    return '执行计划';
  }
  if (eventType === 'agent_step_log') {
    return payload.message || payload.name || existingEvent?.name || '执行日志';
  }
  if (eventType.startsWith('agent_tool') || payload.type === 'TOOL') {
    return payload.name || payload.toolName || existingEvent?.name || '工具调用';
  }
  return payload.name || payload.title || payload.toolName || existingEvent?.name || eventType;
}

function updateAgentProgress(traceInfo: ToolCallInfo, eventType: string) {
  if (eventType === 'agent_run_done') {
    setAgentProgress('');
    return;
  }
  if (traceInfo.status !== 'pending') {
    return;
  }
  setAgentProgress(`正在执行：${traceInfo.name}`);
}

function setAgentProgress(text: string) {
  const lastMessage = getCurrentAssistantMessage();
  if (!lastMessage || hasAssistantContent.value) {
    return;
  }
  lastMessage.agentProgress = text;
  bubbleItems.value = [...bubbleItems.value];
}

function finalizePendingTraceEvents(status: ToolCallInfo['status']) {
  if (!executionEvents.value.some(item => item.status === 'pending')) {
    syncCurrentAssistantToolCalls();
    return;
  }
  executionEvents.value = executionEvents.value.map(item => item.status === 'pending'
    ? {
        ...item,
        status,
        result: item.result || (status === 'success' ? '已完成' : '执行中断'),
      }
    : item);
  syncCurrentAssistantToolCalls();
}

function normalizeTraceStatus(status: unknown, eventType: string): ToolCallInfo['status'] {
  if (eventType === 'agent_step_log' && status !== 'error' && status !== 'failed') {
    return 'success';
  }
  if (eventType.endsWith('_start') || status === 'running') {
    return 'pending';
  }
  if (status === 'error' || status === 'failed') {
    return 'error';
  }
  return 'success';
}

function formatTraceResult(payload: AnyObject, eventType: string) {
  const result = payload.result ?? payload.output ?? payload.response ?? payload.rawResult ?? payload.error;
  if (result !== undefined && result !== null) {
    return typeof result === 'string' ? result : JSON.stringify(result, null, 2);
  }
  if (payload.message) {
    return String(payload.message);
  }
  if (eventType.endsWith('_start')) {
    return null;
  }
  return JSON.stringify({ event: eventType, ...payload }, null, 2);
}

function extractTraceInput(payload: AnyObject) {
  return payload.input ?? payload.arguments ?? payload.args ?? payload.params ?? payload.request ?? null;
}

function appendReasoningContent(reasoningContent: string) {
  const lastMessage = bubbleItems.value[bubbleItems.value.length - 1];
  if (lastMessage) {
    lastMessage.thinkingStatus = 'thinking';
    lastMessage.reasoning_content += reasoningContent;
    bubbleItems.value = [...bubbleItems.value];
  }
}

function handleMcpEvent(dataObj: AnyObject) {
  try {
    const content = typeof dataObj.content === 'string'
      ? JSON.parse(dataObj.content)
      : (dataObj.content || dataObj);

    if (content?.runId && activeAgentRunId.value && content.runId !== activeAgentRunId.value) {
      return;
    }

    const toolName = content.name || content.toolName || 'Unknown Tool';
    const toolStatus = content.status || 'pending';
    const toolResult = content.result ?? content.output ?? content.rawResult ?? content.error ?? null;
    const toolStepId = content.stepId || content.id || `mcp:${toolName}`;
    const normalizedStatus = normalizeTraceStatus(toolStatus, toolStatus === 'pending' ? 'tool_call_start' : 'tool_call_done');
    const traceInfo: ToolCallInfo = {
      key: ++toolCallKeyCounter,
      name: toolName,
      status: normalizedStatus,
      result: toolResult,
      timestamp: content.timestamp || Date.now(),
      type: 'TOOL',
      runId: content.runId,
      stepId: toolStepId,
      input: extractTraceInput(content),
      order: ++traceOrderCounter,
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
        order: updatedEvents[index].order,
      };
      executionEvents.value = updatedEvents;
    }
    else {
      executionEvents.value = [...executionEvents.value, traceInfo];
    }

    syncCurrentAssistantToolCalls();
    bubbleItems.value = [...bubbleItems.value];
  }
  catch (err) {
    console.error('[SSE] MCP 事件解析失败:', err);
  }
}

function getCurrentAssistantMessage() {
  for (let i = bubbleItems.value.length - 1; i >= 0; i--) {
    const item = bubbleItems.value[i];
    if (isAssistantRole(item)) {
      return item;
    }
  }
  return null;
}

function syncCurrentAssistantToolCalls() {
  const assistantMessage = getCurrentAssistantMessage();
  if (!assistantMessage) {
    return;
  }
  assistantMessage.toolCalls = [...executionEvents.value].sort((a, b) => {
    const orderA = a.order ?? a.timestamp ?? 0;
    const orderB = b.order ?? b.timestamp ?? 0;
    return orderA - orderB;
  });
}

function isAssistantStreaming(item: MessageItem) {
  if (!isAssistantRole(item)) {
    return false;
  }
  const lastMessage = bubbleItems.value[bubbleItems.value.length - 1];
  return Boolean(isLoading.value && lastMessage?.key === item.key);
}

function isAssistantRole(item: MessageItem) {
  return item.role === 'system' || item.role === 'ai';
}

function completedToolCallCount(toolCalls?: ToolCallInfo[]) {
  return toolCalls?.filter(trace => trace.type !== 'LOG' && trace.status !== 'pending').length ?? 0;
}

function shouldShowReasoningPanel(item: MessageItem) {
  return Boolean(item.agentMode || item.reasoning_content || item.toolCalls?.length || item.agentProgress);
}

function reasoningStateText(item: MessageItem) {
  if (isAssistantStreaming(item)) {
    return '进行中';
  }
  if (item.toolCalls?.some(trace => trace.status === 'error')) {
    return '有失败步骤';
  }
  return '已完成';
}

function reasoningLeadText(item: MessageItem) {
  if (item.agentProgress) {
    return item.agentProgress;
  }
  if (item.agentMode && isAssistantStreaming(item)) {
    return '正在连接业务协作链路，准备分析问题并选择工具。';
  }
  if (item.reasoning_content && isAssistantStreaming(item)) {
    return '正在准备响应，稍后会展示分析和执行步骤。';
  }
  return '';
}

function shouldUseInternalAgent(content: string) {
  if (chatSenderRef.value?.isReasoningEnabled) {
    return true;
  }
  const text = content.toLowerCase();
  const businessKeywords = [
    '客户',
    '公司',
    '联系人',
    '合同',
    '商机',
    '报价',
    '回款',
    '付款',
    '跟进',
    '拜访',
    '工单',
    '生产',
    '进度',
    '节点',
    '阶段',
    '物料',
    '库存',
    '采购',
    '收料',
    '发料',
    '入库',
    '出库',
    'crm',
    'mes',
    'srm',
    'wms',
  ];
  return businessKeywords.some(keyword => text.includes(keyword));
}

function handleContentChunk(content: string) {
  const lastIndex = bubbleItems.value.length - 1;
  const lastMessage = bubbleItems.value[lastIndex];
  if (!lastMessage) {
    return;
  }

  if (content) {
    hasAssistantContent.value = true;
    lastMessage.agentProgress = '';
    lastMessage.loading = false;
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
    const lastMessage = bubbleItems.value[bubbleItems.value.length - 1];
    lastMessage.typing = false;
    lastMessage.loading = false;
    lastMessage.agentProgress = '';
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

function addMessage(message: string, isUser: boolean, options: { agentMode?: boolean } = {}) {
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
    loading: false,
    content: message || '',
    reasoning_content: '',
    toolCalls: [],
    thinkingStatus: 'start',
    thinlCollapse: false,
    noStyle: !isUser,
    agentMode: options.agentMode,
    agentProgress: !isUser && options.agentMode
      ? '正在连接业务协作链路，准备分析问题并选择工具。'
      : '',
  };
  bubbleItems.value.push(obj);
}

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
  <div class="chat-with-id-container" translate="no">
    <div class="chat-warp">
      <BubbleList ref="bubbleListRef" :list="bubbleItems" max-height="calc(100vh - 216px)">
        <template #content="{ item }">
          <div v-if="isAssistantRole(item)" class="assistant-answer-card">
            <div v-if="shouldShowReasoningPanel(item)" class="reasoning-panel" :class="{ expanded: !item.thinlCollapse }">
              <button type="button" class="reasoning-toggle" @click="item.thinlCollapse = !item.thinlCollapse">
                <span class="reasoning-dot" :class="{ active: item.thinkingStatus === 'thinking' || isAssistantStreaming(item) }" />
                <span>思考 / 执行过程</span>
                <span v-if="item.toolCalls?.length" class="reasoning-count">
                  {{ completedToolCallCount(item.toolCalls) }}/{{ item.toolCalls.length }}
                </span>
                <span class="reasoning-state">{{ reasoningStateText(item) }}</span>
                <el-icon class="reasoning-arrow" :class="{ open: !item.thinlCollapse }">
                  <ArrowDown />
                </el-icon>
              </button>
              <div v-show="!item.thinlCollapse" class="reasoning-content">
                <div v-if="reasoningLeadText(item)" class="reasoning-lead">
                  <span class="agent-progress-dot" />
                  <span>{{ reasoningLeadText(item) }}</span>
                </div>

                <div v-if="item.toolCalls?.length" class="reasoning-timeline">
                  <div
                    v-for="trace in item.toolCalls"
                    :key="trace.key || trace.stepId"
                    class="reasoning-timeline-row"
                    :class="trace.status"
                  >
                    <span class="timeline-marker" />
                    <div class="timeline-card">
                      <ToolCallCard
                        :tool-info="trace"
                        @confirm-draft="handleConfirmDraft"
                      />
                    </div>
                  </div>
                </div>

                <div v-if="item.reasoning_content" class="reasoning-text">
                  {{ item.reasoning_content }}
                </div>
              </div>
            </div>
            <XMarkdown
              v-if="item.content"
              :markdown="item.content"
              :code-x-render="codeXRender"
              class="markdown-body"
              :themes="{ light: 'github-light', dark: 'github-dark' }"
              default-theme-mode="light"
            />
            <span v-if="item.content && isAssistantStreaming(item)" class="streaming-caret" />
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

.agent-progress-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #4f7cff;
  box-shadow: 0 0 0 0 rgb(79 124 255 / 45%);
  animation: agent-progress-pulse 1.3s ease-in-out infinite;
  flex-shrink: 0;
}

@keyframes agent-progress-pulse {
  0% {
    box-shadow: 0 0 0 0 rgb(79 124 255 / 45%);
  }
  70% {
    box-shadow: 0 0 0 8px rgb(79 124 255 / 0%);
  }
  100% {
    box-shadow: 0 0 0 0 rgb(79 124 255 / 0%);
  }
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
  justify-content: flex-start;
  gap: 0;
  width: 100%;
  max-width: none;
  height: 100%;
  padding: 0 28px 0 18px;
  box-sizing: border-box;
  background:
    linear-gradient(90deg, #f7f9fc 0, #ffffff 230px, #ffffff 100%),
    #ffffff;

  .chat-warp {
    display: flex;
    flex-direction: column;
    justify-content: space-between;
    flex: 1 1 auto;
    min-width: 0;
    max-width: 1050px;
    height: calc(100vh - 60px);
    margin: 0;

    .thinking-chain-warp {
      margin-bottom: 12px;
    }

    .sender-wrapper {
      display: flex;
      flex-direction: column;
      gap: 10px;
      width: 100%;
      max-width: 900px;
      margin: 0 0 18px;

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
        border-radius: 8px;
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
      padding: 22px 0 12px;
    }
    .el-bubble {
      padding: 0 0 22px;
    }
    .el-bubble-start .el-bubble-content,
    .el-bubble-start .el-bubble-content-wrapper {
      width: 100%;
      max-width: 100% !important;
    }
    .el-bubble-start .el-bubble-content-wrapper {
      justify-content: flex-start;
    }
    .el-bubble-end .el-bubble-content {
      max-width: min(680px, 84%);
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

@media (max-width: 1180px) {
  .chat-with-id-container {
    padding: 0 16px;
  }
}

.assistant-answer-card {
  width: min(100%, 920px);
  margin: 0;
  padding: 0;
  overflow: hidden;
  background: #ffffff;
  border: 1px solid #dfe7f2;
  border-radius: 10px;
  box-shadow: 0 12px 34px rgb(15 23 42 / 8%);

  .reasoning-panel {
    margin: 12px 14px 0;
    border: 1px solid #dbe4ef;
    border-radius: 8px;
    background: #f8fafc;
    overflow: hidden;

    &.expanded {
      background: #ffffff;
    }
  }

  .reasoning-toggle {
    width: 100%;
    min-height: 38px;
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 8px 11px;
    border: 0;
    background: transparent;
    cursor: pointer;
    color: #334155;
    font-size: 13px;
    font-weight: 700;
    text-align: left;
  }

  .reasoning-dot {
    width: 7px;
    height: 7px;
    border-radius: 50%;
    background: #94a3b8;

    &.active {
      background: #f59e0b;
      box-shadow: 0 0 0 4px rgb(245 158 11 / 16%);
    }
  }

  .reasoning-state {
    margin-left: auto;
    color: #64748b;
    font-size: 12px;
    font-weight: 650;
  }

  .reasoning-count {
    margin-left: auto;
    padding: 2px 7px;
    color: #475569;
    background: #eef2f7;
    border-radius: 999px;
    font-size: 12px;
    font-weight: 700;
  }

  .reasoning-count + .reasoning-state {
    margin-left: 0;
  }

  .reasoning-arrow {
    color: #64748b;
    transition: transform 0.2s ease;

    &.open {
      transform: rotate(180deg);
    }
  }

  .reasoning-content {
    max-height: 420px;
    overflow: auto;
    padding: 0 12px 12px;
    white-space: pre-wrap;
    word-break: break-word;
    color: #475569;
    font-size: 13px;
    line-height: 1.72;
  }

  .reasoning-lead {
    display: inline-flex;
    align-items: center;
    gap: 8px;
    max-width: 100%;
    margin: 0 0 10px;
    padding: 8px 10px;
    color: #42526b;
    background: #f8fafc;
    border: 1px solid #e4ebf5;
    border-radius: 7px;
    font-size: 12px;
    line-height: 1.5;
  }

  .reasoning-timeline {
    position: relative;
    display: flex;
    flex-direction: column;
    gap: 8px;
    margin: 0 0 10px;

    &::before {
      position: absolute;
      top: 12px;
      bottom: 12px;
      left: 7px;
      width: 1px;
      content: '';
      background: #dbe4ef;
    }
  }

  .reasoning-timeline-row {
    position: relative;
    display: grid;
    grid-template-columns: 16px minmax(0, 1fr);
    gap: 8px;
    align-items: flex-start;

    &.pending .timeline-marker {
      background: #f59e0b;
      box-shadow: 0 0 0 4px rgb(245 158 11 / 14%);
      animation: agent-progress-pulse 1.25s ease-in-out infinite;
    }

    &.success .timeline-marker {
      background: #22c55e;
    }

    &.error .timeline-marker {
      background: #ef4444;
    }
  }

  .timeline-marker {
    position: relative;
    z-index: 1;
    width: 7px;
    height: 7px;
    margin: 14px 0 0 4px;
    border-radius: 50%;
    background: #94a3b8;
    box-shadow: 0 0 0 3px #ffffff;
  }

  .timeline-card {
    min-width: 0;
  }

  .reasoning-text {
    padding-top: 10px;
    border-top: 1px dashed #dbe4ef;
    color: #475569;
  }

  :deep(.tool-call-card) {
    border-color: #e2e8f0;
  }

  :deep(.tool-call-card .card-header) {
    min-height: 46px;
  }

  :deep(.tool-call-card .tool-icon) {
    width: 22px;
    height: 22px;
    background: #eef4ff;
    color: #2563eb;
    box-shadow: none;
  }

  :deep(.tool-call-card .status-tag) {
    height: 20px;
  }

  :deep(.tool-call-card .card-content) {
    padding-left: 41px;
  }

  @media (max-width: 640px) {
    .reasoning-toggle {
      flex-wrap: wrap;
      align-items: center;
    }

    .reasoning-count {
      margin-left: 0;
    }

    .reasoning-state {
      margin-left: auto;
    }

    .reasoning-content {
      padding: 0 10px 10px;
    }

    .reasoning-timeline-row {
      grid-template-columns: 12px minmax(0, 1fr);
      gap: 6px;
    }
  }

  :deep(.elx-xmarkdown-container) {
    padding: 12px 14px 16px;
    box-sizing: border-box;
    width: 100%;
    max-width: 100%;
    overflow-x: hidden;
  }

  :deep(.markdown-body) {
    color: #172033;
    font-size: 13px;
    line-height: 1.58;
    width: 100%;
    max-width: 100%;
    overflow-x: auto;
    word-break: break-word;
  }

  :deep(.markdown-body p) {
    margin: 0 0 8px;
  }

  :deep(.markdown-body strong) {
    color: #0f172a;
    font-weight: 800;
  }

  :deep(.markdown-body mark) {
    padding: 1px 5px;
    border-radius: 5px;
    color: #b42318;
    background: #fff1f0;
    box-decoration-break: clone;
    font-weight: 800;
  }

  :deep(.markdown-body h1),
  :deep(.markdown-body h2),
  :deep(.markdown-body h3),
  :deep(.markdown-body h4) {
    margin: 14px 0 8px;
    color: #0f172a;
    line-height: 1.28;
    letter-spacing: 0;
  }

  :deep(.markdown-body h1) {
    display: inline-flex;
    align-items: center;
    gap: 8px;
    padding: 4px 0 8px;
    font-size: 17px;
  }

  :deep(.markdown-body h2) {
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 7px 10px;
    border: 1px solid #e2e8f0;
    border-left: 3px solid #2563eb;
    border-radius: 7px;
    background: #f8fbff;
    font-size: 14px;
  }

  :deep(.markdown-body h3) {
    font-size: 13px;
    font-weight: 800;
  }

  :deep(.markdown-body h2 + table),
  :deep(.markdown-body h3 + table) {
    margin-top: 8px;
  }

  :deep(.markdown-body ul),
  :deep(.markdown-body ol) {
    padding-left: 18px;
    margin: 6px 0 10px;
  }

  :deep(.markdown-body li) {
    margin: 3px 0;
  }

  :deep(.markdown-body table) {
    display: table;
    width: 100%;
    min-width: max(100%, 700px);
    max-width: none;
    margin: 9px 0 12px;
    border-collapse: separate;
    border-spacing: 0;
    overflow: hidden;
    border: 1px solid #edf1f6;
    border-radius: 8px;
    font-size: 12px;
    table-layout: auto;
    box-shadow: 0 8px 18px rgb(15 23 42 / 4%);
  }

  :deep(.markdown-body thead th) {
    background: #f7faff;
    color: #334155;
    font-weight: 800;
  }

  :deep(.markdown-body th),
  :deep(.markdown-body td) {
    padding: 6px 10px;
    border: 0;
    border-bottom: 1px solid #eef2f7;
    color: #1f2937;
    vertical-align: top;
    white-space: normal;
    word-break: normal;
    overflow-wrap: anywhere;
    line-height: 1.45;
  }

  :deep(.markdown-body td:first-child),
  :deep(.markdown-body th:first-child) {
    width: 132px;
    background: #fbfdff;
    color: #334155;
    font-weight: 760;
    white-space: nowrap;
  }

  :deep(.markdown-body th + th),
  :deep(.markdown-body td + td) {
    border-left: 1px solid rgb(226 232 240 / 55%);
  }

  :deep(.markdown-body thead th:not(:last-child)),
  :deep(.markdown-body tbody td:not(:last-child)) {
    white-space: nowrap;
  }

  :deep(.markdown-body td:last-child),
  :deep(.markdown-body th:last-child) {
    white-space: normal;
  }

  :deep(.markdown-body tbody tr:last-child td) {
    border-bottom: 0;
  }

  :deep(.markdown-body tbody tr:nth-child(even) td) {
    background: #fcfdff;
  }

  :deep(.markdown-body tbody tr:nth-child(even) td:first-child) {
    background: #f8fbff;
  }

  :deep(.markdown-body tbody tr:hover td) {
    background: #f7fbff;
  }

  :deep(.markdown-body tbody tr:hover td:first-child) {
    background: #f2f7ff;
  }

  :deep(.markdown-body table strong) {
    color: #a7372f;
    font-weight: 850;
    white-space: nowrap;
  }

  :deep(.markdown-body code:not(pre code)) {
    padding: 1px 5px;
    border: 1px solid #dbe4ef;
    border-radius: 5px;
    background: #f6f8fb;
    color: #0f3f8a;
    font-size: 11.5px;
    font-weight: 700;
  }

  :deep(.markdown-body pre) {
    max-width: 100%;
    overflow-x: auto;
    border-radius: 8px;
  }

  .streaming-caret {
    display: inline-block;
    width: 7px;
    height: 18px;
    margin: 0 0 -3px 16px;
    border-radius: 999px;
    background: #1677ff;
    animation: stream-caret 0.85s steps(2, start) infinite;
  }
}

@keyframes stream-caret {
  0%, 45% {
    opacity: 1;
  }
  46%, 100% {
    opacity: 0.18;
  }
}
</style>
