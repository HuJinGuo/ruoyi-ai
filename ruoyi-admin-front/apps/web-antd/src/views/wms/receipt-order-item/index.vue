<script setup lang="ts">
import XtpCrudPage from '../../xtp/components/xtp-crud-page.vue';
import { wmsReceiptOrderItemConfig } from '../../xtp/data';
</script>

<template>
  <XtpCrudPage :config="wmsReceiptOrderItemConfig" />
</template>
