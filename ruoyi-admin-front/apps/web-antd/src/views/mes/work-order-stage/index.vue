<script setup lang="ts">
import XtpCrudPage from '../../xtp/components/xtp-crud-page.vue';
import { mesWorkOrderStageConfig } from '../../xtp/data';
</script>

<template>
  <XtpCrudPage :config="mesWorkOrderStageConfig" />
</template>
