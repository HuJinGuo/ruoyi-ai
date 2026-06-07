<script setup lang="ts">
import CrmCrudPage from '../components/crm-crud-page.vue';
import { crmFollowRecordConfig } from '../data';
</script>

<template>
  <CrmCrudPage :config="crmFollowRecordConfig" />
</template>
