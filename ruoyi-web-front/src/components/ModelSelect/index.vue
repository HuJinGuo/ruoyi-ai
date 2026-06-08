<!-- 切换模型 -->
<script setup lang="ts">
import type { GetSessionListVO } from '@/api/model/types';
import Popover from '@/components/Popover/index.vue';
import SvgIcon from '@/components/SvgIcon/index.vue';
import { useModelStore } from '@/stores/modules/model';

const modelStore = useModelStore();
const isModelLoading = ref(false);

function getModelLabel(item?: GetSessionListVO) {
  return item?.modelDescribe || item?.modelName || '未命名模型';
}

function isSelectedModel(item: GetSessionListVO) {
  const current = modelStore.currentModelInfo;
  if (current?.id && item.id) {
    return current.id === item.id;
  }
  return item.modelName === current?.modelName;
}

async function loadModelList() {
  if (isModelLoading.value) {
    return;
  }

  isModelLoading.value = true;
  try {
    await modelStore.requestModelList();
  }
  finally {
    isModelLoading.value = false;
  }

  // 设置默认模型
  if (
    modelStore.modelList.length > 0
    && !modelStore.currentModelInfo?.modelName
    && !modelStore.currentModelInfo?.modelDescribe
  ) {
    modelStore.setCurrentModelInfo(modelStore.modelList[0]);
  }
}

onMounted(async () => {
  await loadModelList();
});

const currentModelName = computed(
  () => {
    if (!modelStore.currentModelInfo?.modelName && !modelStore.currentModelInfo?.modelDescribe) {
      return '选择模型';
    }
    return getModelLabel(modelStore.currentModelInfo);
  },
);
const popoverList = computed(() => modelStore.modelList);

/* 弹出面板 开始 */
const popoverStyle = ref({
  width: '200px',
  padding: '4px',
  maxHeight: '240px',
  background: 'var(--el-bg-color, #fff)',
  border: '1px solid var(--el-border-color-light)',
  borderRadius: '8px',
  boxShadow: '0 2px 12px 0 rgba(0, 0, 0, 0.1)',
});
const popoverRef = ref();

// 显示
async function showPopover() {
  // 获取最新的模型列表
  await loadModelList();
}

// 点击
function handleClick(item: GetSessionListVO) {
  modelStore.setCurrentModelInfo(item);
  popoverRef.value?.hide?.();
}
</script>

<template>
  <div class="model-select">
    <Popover
      ref="popoverRef"
      placement="top-start"
      :offset="[4, 0]"
      popover-class="popover-content"
      :popover-style="popoverStyle"
      trigger="clickTarget"
      @show="showPopover"
    >
      <!-- 触发元素插槽 -->
      <template #trigger>
        <div
          class="model-select-box select-none flex items-center gap-4px p-10px rounded-10px cursor-pointer font-size-12px"
        >
          <div class="model-select-box-icon">
            <SvgIcon name="models" size="12" />
          </div>
          <div class="model-select-box-text font-size-12px">
            {{ currentModelName }}
          </div>
        </div>
      </template>

      <div class="popover-content-box">
        <div v-if="isModelLoading" class="model-state">
          查询模型中...
        </div>

        <div v-else-if="!popoverList.length" class="model-state">
          暂无可用模型
        </div>

        <div v-else class="model-list">
          <div
            v-for="item in popoverList"
            :key="item.id || item.modelName"
            class="popover-content-box-items w-full rounded-8px select-none transition-all transition-duration-300 flex items-center hover:cursor-pointer hover:bg-[rgba(0,0,0,.04)]"
          >
            <Popover
              trigger-class="popover-trigger-item-text"
              popover-class="rounded-tooltip"
              placement="right"
              trigger="hover"
              :offset="[12, 0]"
            >
              <template #trigger>
                <div
                  class="popover-content-box-item p-4px font-size-12px text-overflow line-height-16px"
                  :class="{ 'bg-[rgba(0,0,0,.04)] is-select': isSelectedModel(item) }"
                  @click="handleClick(item)"
                >
                  {{ getModelLabel(item) }}
                </div>
              </template>
              <div
                class="popover-content-box-item-text text-wrap max-w-200px rounded-lg p-8px font-size-12px line-height-tight"
              >
                {{ item.remark || item.modelName || '暂无描述' }}
              </div>
            </Popover>
          </div>
        </div>
      </div>
    </Popover>
  </div>
</template>

<style scoped lang="scss">
.model-select-box {
  max-width: 180px;
  background-color: #fff;
  border: 1px solid rgb(0 0 0 / 10%);
  color: rgb(0 0 0 / 85%);
  font-weight: 500;
  transition: all 0.2s ease;

  &:hover {
    background-color: rgb(0 0 0 / 4%);
    border-color: rgb(0 0 0 / 15%);
  }

  // 选中状态（模型始终选中，显示蓝色）
  background: var(--el-color-primary-light-9, rgb(235.9 245.3 255));
  border-color: var(--el-color-primary, #409eff);
  color: var(--el-color-primary, #409eff);
  font-weight: 600;
}

.model-select-box-text {
  min-width: 0;
  max-width: 132px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.popover-content-box-item.is-select {
  font-weight: 700;
  color: var(--el-color-primary, #409eff);
}

.popover-content-box {
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-height: 40px;
  max-height: 232px;
  overflow: hidden auto;

  .model-state {
    padding: 12px;
    color: #8a93a3;
    font-size: 12px;
    line-height: 18px;
    text-align: center;
  }

  .model-list {
    display: flex;
    flex-direction: column;
    gap: 4px;
  }

  .popover-content-box-items {
    :deep() {
      .popover-trigger-item-text {
        width: 100%;
      }
    }
  }

  .popover-content-box-item-text {
    color: white;
    background-color: black;
  }

  // 滚动条样式
  &::-webkit-scrollbar {
    width: 4px;
  }

  &::-webkit-scrollbar-track {
    background: #f5f5f5;
  }

  &::-webkit-scrollbar-thumb {
    background: #cccccc;
    border-radius: 4px;
  }
}
</style>
