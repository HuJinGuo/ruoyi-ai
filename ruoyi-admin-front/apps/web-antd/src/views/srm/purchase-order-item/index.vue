<script setup lang="ts">
import XtpCrudPage from '../../xtp/components/xtp-crud-page.vue';
import { srmPurchaseOrderItemConfig } from '../../xtp/data';
</script>

<template>
  <XtpCrudPage :config="srmPurchaseOrderItemConfig" />
</template>
