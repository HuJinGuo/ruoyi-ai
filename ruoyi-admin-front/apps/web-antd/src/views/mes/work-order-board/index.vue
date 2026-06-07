<script setup lang="ts">
import { onMounted, ref } from 'vue';

import { Page } from '@vben/common-ui';

import { Button, Card, Progress, Select, Space, Timeline } from 'ant-design-vue';

import { mesWorkOrderApi, xtpFlowApi } from '#/api/xtp';

const workOrders = ref<any[]>([]);
const selectedWorkOrderId = ref<number>();
const stages = ref<any[]>([]);
const loading = ref(false);

async function loadWorkOrders() {
  workOrders.value = await mesWorkOrderApi.options();
  selectedWorkOrderId.value = workOrders.value[0]?.workOrderId;
  await loadStages();
}

async function loadStages() {
  if (!selectedWorkOrderId.value) return;
  loading.value = true;
  try {
    stages.value = await xtpFlowApi.listStages(selectedWorkOrderId.value) as any[];
  } finally {
    loading.value = false;
  }
}

function statusColor(status: string) {
  if (status === 'FINISHED') return 'green';
  if (status === 'PROCESSING') return 'blue';
  if (status === 'PAUSE') return 'orange';
  return 'gray';
}

onMounted(loadWorkOrders);
</script>

<template>
  <Page :auto-content-height="true">
    <Card :loading="loading" title="项目进度看板">
      <Space class="mb-4">
        <Select
          v-model:value="selectedWorkOrderId"
          :options="workOrders.map((item) => ({ label: [item.workOrderCode, item.projectName].filter(Boolean).join(' / '), value: item.workOrderId }))"
          class="w-[360px]"
          show-search
          @change="loadStages"
        />
        <Button type="primary" @click="loadStages">刷新</Button>
      </Space>
      <Progress
        :percent="stages.length ? Math.round((stages.filter((item) => item.status === 'FINISHED').length / stages.length) * 100) : 0"
        class="mb-6 max-w-[520px]"
      />
      <Timeline>
        <Timeline.Item
          v-for="stage in stages"
          :key="stage.workOrderStageId"
          :color="statusColor(stage.status)"
        >
          <div class="flex flex-col gap-1">
            <span class="font-medium">{{ stage.stageName }} / {{ stage.stageCode }}</span>
            <span class="text-sm text-gray-500">{{ stage.status }} {{ stage.remark ? `- ${stage.remark}` : '' }}</span>
          </div>
        </Timeline.Item>
      </Timeline>
    </Card>
  </Page>
</template>
