<script setup lang="ts">
import type { VbenFormProps } from '@vben/common-ui';

import type { VxeGridProps } from '#/adapter/vxe-table';

import type { CrmEntity } from '#/api/crm/model';

import { onMounted, ref } from 'vue';

import { Page, useVbenDrawer } from '@vben/common-ui';
import { getVxePopupContainer } from '@vben/utils';

import { Modal, Popconfirm, Space } from 'ant-design-vue';

import { useVbenVxeGrid, vxeCheckboxChecked } from '#/adapter/vxe-table';
import { commonDownloadExcel } from '#/utils/file/download';

import {
  getEmptyLookups,
  loadCrmLookups,
  type CrmLookups,
  type CrmPageConfig,
} from '../data';
import CrmEntityDrawer from './crm-entity-drawer.vue';

const props = defineProps<{
  config: CrmPageConfig;
}>();

const lookups = ref<CrmLookups>(getEmptyLookups());

const formOptions: VbenFormProps = {
  commonConfig: {
    componentProps: {
      allowClear: true,
    },
    labelWidth: 92,
  },
  schema: props.config.querySchema(lookups.value),
  wrapperClass: 'grid-cols-1 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4',
};

const gridOptions: VxeGridProps = {
  checkboxConfig: {
    highlight: true,
    reserve: true,
    trigger: 'cell',
  },
  columns: props.config.columns,
  height: 'auto',
  keepSource: true,
  pagerConfig: {},
  proxyConfig: {
    ajax: {
      query: async ({ page }, formValues = {}) => {
        return await props.config.api.list({
          pageNum: page.currentPage,
          pageSize: page.pageSize,
          ...formValues,
        });
      },
    },
  },
  rowConfig: {
    keyField: props.config.rowKey,
  },
  id: `crm-${props.config.rowKey}-index`,
};

const [BasicTable, tableApi] = useVbenVxeGrid({
  formOptions,
  gridOptions,
});

const [EntityDrawer, drawerApi] = useVbenDrawer({
  connectedComponent: CrmEntityDrawer,
});

onMounted(async () => {
  lookups.value = await loadCrmLookups();
  tableApi.formApi.updateSchema(props.config.querySchema(lookups.value));
});

function getId(record: Record<string, any>) {
  return record[props.config.rowKey];
}

function handleAdd() {
  drawerApi.setData({ config: props.config, lookups: lookups.value });
  drawerApi.open();
}

function handleEdit(record: CrmEntity) {
  drawerApi.setData({
    config: props.config,
    id: getId(record),
    lookups: lookups.value,
  });
  drawerApi.open();
}

async function handleDelete(row: CrmEntity) {
  await props.config.api.remove([getId(row)]);
  await tableApi.query();
}

function handleMultiDelete() {
  const rows = tableApi.grid.getCheckboxRecords();
  const ids = rows.map((row: Record<string, any>) => getId(row));
  Modal.confirm({
    content: `确认删除选中的${ids.length}条记录吗？`,
    okType: 'danger',
    onOk: async () => {
      await props.config.api.remove(ids);
      await tableApi.query();
    },
    title: '提示',
  });
}

function handleDownloadExcel() {
  commonDownloadExcel(
    props.config.api.export,
    props.config.exportName,
    tableApi.formApi.form.values,
  );
}
</script>

<template>
  <Page :auto-content-height="true">
    <BasicTable :table-title="config.title">
      <template #toolbar-tools>
        <Space>
          <a-button
            v-access:code="[`${config.permission}:export`]"
            @click="handleDownloadExcel"
          >
            {{ $t('pages.common.export') }}
          </a-button>
          <a-button
            :disabled="!vxeCheckboxChecked(tableApi)"
            danger
            type="primary"
            v-access:code="[`${config.permission}:remove`]"
            @click="handleMultiDelete"
          >
            {{ $t('pages.common.delete') }}
          </a-button>
          <a-button
            type="primary"
            v-access:code="[`${config.permission}:add`]"
            @click="handleAdd"
          >
            {{ $t('pages.common.add') }}
          </a-button>
        </Space>
      </template>
      <template #action="{ row }">
        <Space>
          <GhostButton
            v-access:code="[`${config.permission}:edit`]"
            @click="handleEdit(row)"
          >
            {{ $t('pages.common.edit') }}
          </GhostButton>
          <Popconfirm
            :get-popup-container="getVxePopupContainer"
            placement="left"
            title="确认删除？"
            @confirm="handleDelete(row)"
          >
            <GhostButton
              danger
              v-access:code="[`${config.permission}:remove`]"
              @click.stop=""
            >
              {{ $t('pages.common.delete') }}
            </GhostButton>
          </Popconfirm>
        </Space>
      </template>
    </BasicTable>
    <EntityDrawer @reload="tableApi.query()" />
  </Page>
</template>
