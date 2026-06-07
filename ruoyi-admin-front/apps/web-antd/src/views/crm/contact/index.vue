<script setup lang="ts">
import CrmCrudPage from '../components/crm-crud-page.vue';
import { crmContactConfig } from '../data';
</script>

<template>
  <CrmCrudPage :config="crmContactConfig" />
</template>
