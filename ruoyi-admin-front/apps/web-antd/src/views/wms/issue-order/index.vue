<script setup lang="ts">
import XtpCrudPage from '../../xtp/components/xtp-crud-page.vue';
import { wmsIssueOrderConfig } from '../../xtp/data';
</script>

<template>
  <XtpCrudPage :config="wmsIssueOrderConfig" />
</template>
