<script setup lang="ts">
import CrmCrudPage from '../components/crm-crud-page.vue';
import { crmPaymentPlanConfig } from '../data';
</script>

<template>
  <CrmCrudPage :config="crmPaymentPlanConfig" />
</template>
