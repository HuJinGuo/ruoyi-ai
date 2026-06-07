<script setup lang="ts">
import XtpCrudPage from '../../xtp/components/xtp-crud-page.vue';
import { srmPurchaseRequestConfig } from '../../xtp/data';
</script>

<template>
  <XtpCrudPage :config="srmPurchaseRequestConfig" />
</template>
