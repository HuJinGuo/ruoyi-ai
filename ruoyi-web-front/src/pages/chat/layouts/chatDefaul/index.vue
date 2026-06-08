<!-- 默认消息列表页 -->
<script setup lang="ts">
import { ref } from 'vue';
import ChatSender from '@/components/ChatSender/index.vue';
import WelecomeText from '@/components/WelecomeText/index.vue';
import { useUserStore } from '@/stores';
import { useSessionStore } from '@/stores/modules/session';

const userStore = useUserStore();
const sessionStore = useSessionStore();

const senderValue = ref('');
const senderRef = ref<InstanceType<typeof ChatSender> | null>(null);

async function handleSubmit(content: string) {
  localStorage.setItem('chatContent', content);
  localStorage.setItem('enableThinking', String(senderRef.value?.isReasoningEnabled || false));

  senderValue.value = '';
  await sessionStore.createSessionList({
    userId: userStore.userInfo?.userId as number,
    sessionContent: content,
    sessionTitle: content.slice(0, 10),
    remark: content.slice(0, 10),
  });
}
</script>

<template>
  <div class="chat-defaul-wrap">
    <div class="welcome-region">
      <WelecomeText />
    </div>

    <div class="default-sender-wrapper">
      <ChatSender
        ref="senderRef"
        v-model="senderValue"
        @submit="handleSubmit"
      />
    </div>
  </div>
</template>

<style scoped lang="scss">
.chat-defaul-wrap {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  width: 100%;
  max-width: 800px;
  min-height: min(620px, calc(100vh - 96px));
  gap: 32px;
  padding: 24px 0 48px;
  box-sizing: border-box;
}

.welcome-region,
.default-sender-wrapper {
  width: 100%;
}

.welcome-region {
  display: flex;
  justify-content: center;
}

.default-sender-wrapper {
  flex-shrink: 0;
}

@media (max-width: 640px) {
  .chat-defaul-wrap {
    min-height: calc(100vh - 96px);
    gap: 24px;
    padding: 16px 0 32px;
  }
}
</style>
