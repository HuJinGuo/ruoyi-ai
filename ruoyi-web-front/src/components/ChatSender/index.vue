<!-- 公共聊天输入框组件 -->
<script setup lang="ts">
import type { FilesCardProps } from 'vue-element-plus-x/types/FilesCard';
import { nextTick, ref, watch } from 'vue';
import { Sender } from 'vue-element-plus-x';
import { getKnowledgeList } from '@/api/chat';
import FilesSelect from '@/components/FilesSelect/index.vue';
import ModelSelect from '@/components/ModelSelect/index.vue';
import { useChatStore } from '@/stores/modules/chat';
import { useFilesStore } from '@/stores/modules/files';

const props = defineProps<{
  modelValue?: string;
  loading?: boolean;
}>();

const emit = defineEmits<{
  'update:modelValue': [value: string];
  'submit': [content: string];
  'cancel': [];
}>();

const chatStore = useChatStore();
const filesStore = useFilesStore();

const senderValue = computed({
  get: () => props.modelValue || '',
  set: val => emit('update:modelValue', val),
});

const senderRef = ref<InstanceType<typeof Sender> | null>(null);

// 推理开关状态
const isReasoningEnabled = ref(false);

// 知识库列表配置
const knowledgeList = ref<any[]>([]);

// 知识库弹窗状态
const knowledgePopoverRef = ref();
const selectedKnowledgeId = ref<string>('');
const selectedKnowledgeName = ref<string>('知识库');

// 加载知识库列表
async function loadKnowledgeList() {
  try {
    const response = await getKnowledgeList();
    if (response?.rows && Array.isArray(response.rows)) {
      knowledgeList.value = response.rows.map((item: any) => ({
        id: item.id,
        name: item.name,
        icon: 'Document',
      }));
    }
  }
  catch (error) {
    console.error('Failed to load knowledge list:', error);
  }
}

// 插入知识库标签
function insertKnowledgeTag(knowledgeId: string) {
  const knowledge = knowledgeList.value.find(k => k.id === knowledgeId);
  if (knowledge) {
    selectedKnowledgeId.value = knowledgeId;
    selectedKnowledgeName.value = knowledge.name;
    chatStore.setKnowledgeId(knowledgeId);
    // 关闭弹窗
    knowledgePopoverRef.value?.hide();
  }
}

// 清除知识库选择
function clearKnowledgeSelection() {
  selectedKnowledgeId.value = '';
  selectedKnowledgeName.value = '知识库';
  chatStore.setKnowledgeId('');
}

function handleSubmit() {
  emit('submit', senderValue.value);
}

function handleCancel() {
  emit('cancel');
}

function handleDeleteCard(_item: FilesCardProps, index: number) {
  filesStore.deleteFileByIndex(index);
}

// 暴露方法供父组件调用
function openHeader() {
  nextTick(() => {
    senderRef.value?.openHeader();
  });
}

function closeHeader() {
  nextTick(() => {
    senderRef.value?.closeHeader();
  });
}

watch(
  () => filesStore.filesList.length,
  (val) => {
    if (val > 0) {
      openHeader();
    }
    else {
      closeHeader();
    }
  },
);

// 从 store 中同步知识库选择状态
watch(
  () => chatStore.knowledgeId,
  (id) => {
    if (id) {
      const knowledge = knowledgeList.value.find(k => k.id === id);
      if (knowledge) {
        selectedKnowledgeId.value = id;
        selectedKnowledgeName.value = knowledge.name;
      }
    }
    else {
      selectedKnowledgeId.value = '';
      selectedKnowledgeName.value = '知识库';
    }
  },
);

// 组件挂载时加载知识库列表
onMounted(() => {
  loadKnowledgeList();
  // 从 store 中同步知识库选择状态
  if (chatStore.knowledgeId) {
    const knowledge = knowledgeList.value.find(k => k.id === chatStore.knowledgeId);
    if (knowledge) {
      selectedKnowledgeId.value = chatStore.knowledgeId;
      selectedKnowledgeName.value = knowledge.name;
    }
  }
});

defineExpose({
  openHeader,
  closeHeader,
  isReasoningEnabled,
});
</script>

<template>
  <Sender
    ref="senderRef"
    v-model="senderValue"
    class="chat-sender"
    :auto-size="{
      maxRows: 6,
      minRows: 3,
    }"
    variant="updown"
    clearable
    allow-speech
    :loading="loading"
    @submit="handleSubmit"
    @cancel="handleCancel"
  >
    <template #header>
      <div class="sender-header p-12px pt-6px pb-0px">
        <Attachments
          :items="filesStore.filesList"
          :hide-upload="true"
          @delete-card="handleDeleteCard"
        >
          <template #prev-button="{ show, onScrollLeft }">
            <div
              v-if="show"
              class="prev-next-btn left-8px flex-center w-22px h-22px rounded-8px border-1px border-solid border-[rgba(0,0,0,0.08)] c-[rgba(0,0,0,.4)] hover:bg-#f3f4f6 bg-#fff font-size-10px"
              @click="onScrollLeft"
            >
              <el-icon>
                <ArrowLeftBold />
              </el-icon>
            </div>
          </template>

          <template #next-button="{ show, onScrollRight }">
            <div
              v-if="show"
              class="prev-next-btn right-8px flex-center w-22px h-22px rounded-8px border-1px border-solid border-[rgba(0,0,0,0.08)] c-[rgba(0,0,0,.4)] hover:bg-#f3f4f6 bg-#fff font-size-10px"
              @click="onScrollRight"
            >
              <el-icon>
                <ArrowRightBold />
              </el-icon>
            </div>
          </template>
        </Attachments>
      </div>
    </template>
    <template #prefix>
      <div class="sender-prefix-container">
        <!-- 左侧按钮组 -->
        <div class="left-buttons">
          <ModelSelect />

          <!-- 知识库选择下拉菜单 -->
          <el-popover
            ref="knowledgePopoverRef"
            placement="top-start"
            :width="280"
            trigger="click"
            popper-class="knowledge-popover"
          >
            <template #default>
              <div class="knowledge-list-container">
                <div class="knowledge-list-header">
                  <span>选择知识库</span>
                  <button class="clear-btn" @click="clearKnowledgeSelection">
                    取消选择
                  </button>
                </div>
                <div class="knowledge-list">
                  <div
                    v-for="item in knowledgeList"
                    :key="item.id"
                    class="knowledge-item"
                    :class="{ 'is-selected': selectedKnowledgeId === item.id }"
                    @click="insertKnowledgeTag(item.id)"
                  >
                    <div class="item-name">
                      <el-icon>
                        <component :is="item.icon" />
                      </el-icon>
                      {{ item.name }}
                      <el-icon v-if="selectedKnowledgeId === item.id" class="item-check">
                        <Check />
                      </el-icon>
                    </div>
                  </div>
                </div>
              </div>
            </template>
            <template #reference>
              <div class="action-btn knowledge-btn" :class="{ active: selectedKnowledgeId }">
                <el-icon class="action-icon">
                  <DocumentCopy />
                </el-icon>
                <span class="action-text">{{ selectedKnowledgeName }}</span>
              </div>
            </template>
          </el-popover>

          <!-- 智能推理按钮 -->
          <div
            class="action-btn"
            :class="{ active: isReasoningEnabled }"
            @click="isReasoningEnabled = !isReasoningEnabled"
          >
            <el-icon class="action-icon">
              <Operation />
            </el-icon>
            <span class="action-text">智能推理</span>
          </div>
        </div>

        <!-- 右侧上传按钮 -->
        <FilesSelect class="ml-auto" />
      </div>
    </template>
  </Sender>
</template>

<style scoped lang="scss">
.chat-sender {
  width: 100%;
}

// 关键：让 prefix 区域扩展占满整行
:deep(.el-sender-prefix) {
  flex: 1;
  width: 100%;
}

// prefix 容器
.sender-prefix-container {
  display: flex;
  align-items: center;
  width: 100%;
  gap: 8px;
}

// 左侧按钮组
.left-buttons {
  display: flex;
  align-items: center;
  gap: 8px;
}

// 统一按钮样式
.action-btn {
  display: flex;
  gap: 4px;
  align-items: center;
  padding: 10px;
  cursor: pointer;
  user-select: none;
  background-color: #fff;
  border: 1px solid rgb(0 0 0 / 10%);
  border-radius: 10px;
  font-size: 12px;
  font-weight: 500;
  color: rgb(0 0 0 / 85%);
  transition: all 0.2s ease;
  white-space: nowrap;

  &:hover {
    background-color: rgb(0 0 0 / 4%);
    border-color: rgb(0 0 0 / 15%);
  }

  // 选中状态
  &.active {
    background: var(--el-color-primary-light-9, rgb(235.9 245.3 255));
    border-color: var(--el-color-primary, #409eff);
    color: var(--el-color-primary, #409eff);
    font-weight: 600;
  }

  .action-icon {
    width: 12px;
    height: 12px;
    font-size: 12px;
    flex-shrink: 0;
  }

  .action-text {
    font-size: 12px;
    max-width: 80px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
}

// 知识库列表容器
.knowledge-list-container {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

// 知识库列表标题
.knowledge-list-header {
  padding: 12px 16px 8px;
  font-size: 14px;
  font-weight: 600;
  color: #303133;
  border-bottom: 1px solid #ebeef5;
  display: flex;
  align-items: center;
  justify-content: space-between;

  span {
    flex: 1;
  }
}

// 知识库列表
.knowledge-list {
  display: flex;
  flex-direction: column;
  gap: 0;
  max-height: 300px;
  overflow-y: auto;

  .knowledge-item {
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 10px 12px;
    cursor: pointer;
    user-select: none;
    background-color: transparent;
    border-radius: 4px;
    transition: all 0.2s ease;
    position: relative;

    &:hover {
      background-color: #f5f7fa;

      .item-name {
        color: #409eff;

        :deep(.el-icon) {
          color: #409eff;
        }
      }
    }

    &.is-selected {
      background-color: #f0f9ff;
      border-left: 3px solid;
      padding-left: 9px;

      .item-name {
        color: #0057ff;
        font-weight: 500;

        :deep(.el-icon) {
          color: #0057ff;
        }

        .item-check {
          color: #0057ff;
          font-size: 16px;
        }
      }
    }

    .item-name {
      display: flex;
      align-items: center;
      gap: 8px;
      flex: 1;
      font-size: 13px;
      color: #606266;
      transition: all 0.2s ease;
      width: 100%;

      :deep(.el-icon) {
        width: 16px;
        height: 16px;
        font-size: 16px;
        color: #909399;
        flex-shrink: 0;
        transition: color 0.2s ease;
      }

      .item-check {
        margin-left: auto;
        flex-shrink: 0;
      }
    }
  }
}

// 清除按钮
.clear-btn {
  padding: 4px 12px;
  height: auto;
  background-color: transparent;
  border: none;
  border-radius: 4px;
  font-size: 12px;
  color: #909399;
  cursor: pointer;
  user-select: none;
  transition: all 0.2s ease;
  flex-shrink: 0;

  &:hover {
    color: #f56c6c;
    background-color: #fef0f0;
  }

  &:active {
    color: #c81d1d;
    background-color: #fde2e2;
  }
}
</style>
