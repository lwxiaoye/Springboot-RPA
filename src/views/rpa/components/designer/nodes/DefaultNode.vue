<template>
  <div class="node default-node" :class="{ selected, disabled: !data.config?.enabled }">
    <Handle type="target" :position="Position.Left" />
    <div class="node-header">
      <span class="node-icon">{{ data.icon }}</span>
      <span class="node-label">{{ data.name }}</span>
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
</script>

<style scoped>
.node {
  padding: 0;
  border-radius: 8px;
  min-width: 120px;
  background: linear-gradient(135deg, #909399 0%, #a6a9ad 100%);
  color: white;
  box-shadow: 0 2px 8px rgba(144, 147, 153, 0.3);
  transition: all 0.2s;
}

.node.selected {
  box-shadow: 0 4px 16px rgba(144, 147, 153, 0.5);
  transform: scale(1.05);
}

.node.disabled {
  opacity: 0.5;
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
</style>
