<script setup lang="ts">
import XtpCrudPage from '../../xtp/components/xtp-crud-page.vue';
import { engineeringMaterialConfig } from '../../xtp/data';
</script>

<template>
  <XtpCrudPage :config="engineeringMaterialConfig" />
</template>
