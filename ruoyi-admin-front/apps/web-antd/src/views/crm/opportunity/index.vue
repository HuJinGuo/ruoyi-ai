<script setup lang="ts">
import CrmCrudPage from '../components/crm-crud-page.vue';
import { crmOpportunityConfig } from '../data';
</script>

<template>
  <CrmCrudPage :config="crmOpportunityConfig" />
</template>
