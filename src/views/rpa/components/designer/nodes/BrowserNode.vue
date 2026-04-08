<template>
  <div class="node browser-node" :class="{ selected, disabled: !data.config?.enabled }">
    <Handle type="target" :position="Position.Left" />
    <div class="node-header">
      <span class="node-icon">{{ data.icon }}</span>
      <span class="node-label">{{ data.name }}</span>
    </div>
    <div class="node-body">
      <span class="node-info" v-if="data.config?.url">{{ truncateUrl(data.config.url) }}</span>
      <span class="node-info" v-else-if="data.config?.selector">{{ truncateText(data.config.selector, 20) }}</span>
      <span class="node-info" v-else>点击配置...</span>
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

const truncateUrl = (url) => {
  try {
    const u = new URL(url)
    return u.hostname
  } catch {
    return url.substring(0, 15)
  }
}

const truncateText = (text, max) => {
  return text.length > max ? text.substring(0, max) + '...' : text
}
</script>

<style scoped>
.node {
  padding: 0;
  border-radius: 8px;
  min-width: 140px;
  background: linear-gradient(135deg, #409eff 0%, #66b1ff 100%);
  color: white;
  box-shadow: 0 2px 8px rgba(64, 158, 255, 0.3);
  transition: all 0.2s;
}

.node.selected {
  box-shadow: 0 4px 16px rgba(64, 158, 255, 0.5);
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

.node-info {
  font-size: 10px;
  opacity: 0.9;
  display: block;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
</style>
