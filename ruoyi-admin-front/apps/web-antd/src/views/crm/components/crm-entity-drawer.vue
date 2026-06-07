<script setup lang="ts">
import type { CrmLookups, CrmPageConfig } from '../data';

import { computed, ref, shallowRef } from 'vue';

import { useVbenDrawer } from '@vben/common-ui';
import { $t } from '@vben/locales';
import { cloneDeep } from '@vben/utils';

import { useVbenForm } from '#/adapter/form';
import { defaultFormValueGetter, useBeforeCloseDiff } from '#/utils/popup';

const emit = defineEmits<{ reload: [] }>();

const configRef = shallowRef<CrmPageConfig>();
const isUpdate = ref(false);

const title = computed(() => {
  return isUpdate.value ? $t('pages.common.edit') : $t('pages.common.add');
});

const [BasicForm, formApi] = useVbenForm({
  commonConfig: {
    componentProps: {
      class: 'w-full',
    },
    formItemClass: 'col-span-1',
    labelWidth: 92,
  },
  schema: [],
  showDefaultActions: false,
  wrapperClass: 'grid-cols-2 gap-x-4',
});

const { onBeforeClose, markInitialized, resetInitialized } = useBeforeCloseDiff(
  {
    currentGetter: defaultFormValueGetter(formApi),
    initializedGetter: defaultFormValueGetter(formApi),
  },
);

const [BasicDrawer, drawerApi] = useVbenDrawer({
  onBeforeClose,
  onClosed: handleClosed,
  onConfirm: handleConfirm,
  async onOpenChange(isOpen) {
    if (!isOpen) {
      return null;
    }
    drawerApi.drawerLoading(true);
    const { config, id, lookups } = drawerApi.getData() as {
      config: CrmPageConfig;
      id?: number | string;
      lookups: CrmLookups;
    };
    configRef.value = config;
    isUpdate.value = !!id;
    formApi.setState((prev) => ({
      ...prev,
      schema: config.drawerSchema(lookups),
    }));
    await formApi.resetForm();
    if (isUpdate.value && id) {
      const record = await config.api.info(id);
      await formApi.setValues(record);
    }
    await markInitialized();
    drawerApi.drawerLoading(false);
  },
});

async function handleConfirm() {
  const config = configRef.value;
  if (!config) {
    return;
  }
  try {
    drawerApi.lock(true);
    const { valid } = await formApi.validate();
    if (!valid) {
      return;
    }
    const data = cloneDeep(await formApi.getValues());
    await (isUpdate.value ? config.api.update(data) : config.api.add(data));
    resetInitialized();
    emit('reload');
    drawerApi.close();
  } catch (error) {
    console.error(error);
  } finally {
    drawerApi.lock(false);
  }
}

async function handleClosed() {
  await formApi.resetForm();
  resetInitialized();
}
</script>

<template>
  <BasicDrawer :title="title" class="w-[720px]">
    <BasicForm />
  </BasicDrawer>
</template>
