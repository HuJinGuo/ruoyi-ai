<script setup lang="ts">
import CrmCrudPage from '../components/crm-crud-page.vue';
import { crmQuoteConfig } from '../data';
</script>

<template>
  <CrmCrudPage :config="crmQuoteConfig" />
</template>
