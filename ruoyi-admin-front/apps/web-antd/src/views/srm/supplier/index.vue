<script setup lang="ts">
import XtpCrudPage from '../../xtp/components/xtp-crud-page.vue';
import { srmSupplierConfig } from '../../xtp/data';
</script>

<template>
  <XtpCrudPage :config="srmSupplierConfig" />
</template>
