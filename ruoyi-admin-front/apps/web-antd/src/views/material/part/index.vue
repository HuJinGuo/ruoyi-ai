<script setup lang="ts">
import XtpCrudPage from '../../xtp/components/xtp-crud-page.vue';
import { materialPartConfig } from '../../xtp/data';
</script>

<template>
  <XtpCrudPage :config="materialPartConfig" />
</template>
