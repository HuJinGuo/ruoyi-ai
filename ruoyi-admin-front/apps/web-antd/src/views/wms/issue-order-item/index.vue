<script setup lang="ts">
import XtpCrudPage from '../../xtp/components/xtp-crud-page.vue';
import { wmsIssueOrderItemConfig } from '../../xtp/data';
</script>

<template>
  <XtpCrudPage :config="wmsIssueOrderItemConfig" />
</template>
