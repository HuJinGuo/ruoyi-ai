<script setup lang="ts">
import XtpCrudPage from '../../xtp/components/xtp-crud-page.vue';
import { wmsReceiptOrderConfig } from '../../xtp/data';
</script>

<template>
  <XtpCrudPage :config="wmsReceiptOrderConfig" />
</template>
