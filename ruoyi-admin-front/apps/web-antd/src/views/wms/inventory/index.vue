<script setup lang="ts">
import XtpCrudPage from '../../xtp/components/xtp-crud-page.vue';
import { wmsInventoryConfig } from '../../xtp/data';
</script>

<template>
  <XtpCrudPage :config="wmsInventoryConfig" />
</template>
