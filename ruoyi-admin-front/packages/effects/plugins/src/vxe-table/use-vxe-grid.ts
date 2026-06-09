import type { VxeGridSlots, VxeGridSlotTypes } from 'vxe-table';

import type { Component, SlotsType } from 'vue';

import type { BaseFormComponentType } from '@vben-core/form-ui';

import type { ExtendedVxeGridApi, VxeGridProps } from './types';

import { defineComponent, h, onBeforeUnmount } from 'vue';

import { useStore } from '@vben-core/shared/store';

import { VxeGridApi } from './api';
import VxeGrid from './use-vxe-grid.vue';

type VbenVxeGridComponent = Component;

export type VbenVxeGridApi<
  T extends Record<string, any> = any,
  D extends BaseFormComponentType = BaseFormComponentType,
> = ExtendedVxeGridApi<T, D>;

export type UseVbenVxeGridReturn<
  T extends Record<string, any> = any,
  D extends BaseFormComponentType = BaseFormComponentType,
> = readonly [VbenVxeGridComponent, VbenVxeGridApi<T, D>];

interface ShallowVxeGridOptions {
  class?: unknown;
  formOptions?: unknown;
  gridClass?: unknown;
  gridEvents?: unknown;
  gridOptions?: unknown;
  separator?: unknown;
  showSearchForm?: boolean;
  tableTitle?: string;
  tableTitleHelp?: string;
}

type FilteredSlots<T> = {
  [K in keyof VxeGridSlots<T> as K extends 'form'
    ? never
    : K]: VxeGridSlots<T>[K];
};

export function useVbenVxeGrid<
  T extends Record<string, any> = any,
  D extends BaseFormComponentType = BaseFormComponentType,
>(options: ShallowVxeGridOptions): UseVbenVxeGridReturn<T, D> {
  // const IS_REACTIVE = isReactive(options);
  const api = new VxeGridApi(options as VxeGridProps<T, D>);
  const extendedApi: ExtendedVxeGridApi<T, D> = api as ExtendedVxeGridApi<T, D>;
  extendedApi.useStore = (selector) => {
    return useStore(api.store, selector);
  };

  const Grid = defineComponent(
    (props: VxeGridProps<T>, { attrs, slots }) => {
      onBeforeUnmount(() => {
        api.unmount();
      });
      const gridProps = { ...props, ...attrs } as Partial<VxeGridProps<T, D>>;
      api.setState(gridProps);
      return () => h(VxeGrid, { ...gridProps, api: extendedApi }, slots);
    },
    {
      name: 'VbenVxeGrid',
      inheritAttrs: false,
      slots: Object as SlotsType<
        {
          // 表格标题
          'table-title': undefined;
          // 工具栏左侧部分
          'toolbar-actions': VxeGridSlotTypes.DefaultSlotParams<T>;
          // 工具栏右侧部分
          'toolbar-tools': VxeGridSlotTypes.DefaultSlotParams<T>;
        } & FilteredSlots<T>
      >,
    },
  );
  // Add reactivity support
  // if (IS_REACTIVE) {
  //   watch(
  //     () => options,
  //     () => {
  //       api.setState(options);
  //     },
  //     { immediate: true },
  //   );
  // }

  return [Grid as VbenVxeGridComponent, extendedApi] as const;
}

export type UseVbenVxeGrid = typeof useVbenVxeGrid;
