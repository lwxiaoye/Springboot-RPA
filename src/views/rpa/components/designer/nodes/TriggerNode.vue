<template>
  <div class="node trigger-node" :class="{ selected, disabled: !data.config?.enabled }">
    <Handle type="target" :position="Position.Left" />
    <div class="node-header">
      <span class="node-icon">{{ data.icon }}</span>
      <span class="node-label">{{ data.name }}</span>
    </div>
    <div class="node-body">
      <span class="node-type">{{ getTriggerType() }}</span>
    </div>
    <Handle type="source" :position="Position.Right" />
  </div>
</template>

<script setup>
import { Position, Handle } from '@vue-flow/core'

const props = defineProps({
  data: { type: Object, required: true },
  selected: { type: Boolean, default: false }
})

const getTriggerType = () => {
  const types = {
    trigger_schedule: '定时',
    trigger_api: 'API',
    trigger_queue: '队列',
    trigger_file: '文件',
    trigger_webhook: 'Webhook',
    trigger_mq: '消息'
  }
  return types[props.data.type] || '触发'
}
</script>

<style scoped>
.node {
  padding: 0;
  border-radius: 8px;
  min-width: 120px;
  background: linear-gradient(135deg, #67c23a 0%, #85ce61 100%);
  color: white;
  box-shadow: 0 2px 8px rgba(103, 194, 58, 0.3);
  transition: all 0.2s;
}

.node.selected {
  box-shadow: 0 4px 16px rgba(103, 194, 58, 0.5);
  transform: scale(1.05);
}

.node.disabled {
  opacity: 0.5;
  background: linear-gradient(135deg, #909399 0%, #a6a9ad 100%);
}

.node-header {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 12px;
  background: rgba(0, 0, 0, 0.1);
  border-radius: 8px 8px 0 0;
}

.node-icon {
  font-size: 14px;
}

.node-label {
  font-size: 12px;
  font-weight: 500;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.node-body {
  padding: 6px 12px;
}

.node-type {
  font-size: 10px;
  opacity: 0.8;
}
</style>
