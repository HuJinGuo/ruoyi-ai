<script setup lang="ts">
import CrmCrudPage from '../components/crm-crud-page.vue';
import { crmCustomerConfig } from '../data';
</script>

<template>
  <CrmCrudPage :config="crmCustomerConfig" />
</template>
