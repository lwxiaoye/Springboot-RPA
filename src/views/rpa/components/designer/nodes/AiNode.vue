<template>
  <div class="node ai-node" :class="{ selected, disabled: !data.config?.enabled }">
    <Handle type="target" :position="Position.Left" />
    <div class="node-header">
      <span class="node-icon">{{ data.icon }}</span>
      <span class="node-label">{{ data.name }}</span>
      <el-tag size="small" type="danger" class="ai-badge">AI</el-tag>
    </div>
    <div class="node-body">
      <span class="node-info" v-if="data.config?.imagePath">{{ getFileName(data.config.imagePath) }}</span>
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

const getFileName = (path) => {
  if (!path) return ''
  const parts = path.split(/[/\\]/)
  return parts[parts.length - 1]
}
</script>

<style scoped>
.node {
  padding: 0;
  border-radius: 8px;
  min-width: 150px;
  background: linear-gradient(135deg, #f56c6c 0%, #f78989 100%);
  color: white;
  box-shadow: 0 2px 8px rgba(245, 108, 108, 0.3);
  transition: all 0.2s;
}

.node.selected {
  box-shadow: 0 4px 16px rgba(245, 108, 108, 0.5);
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

.ai-badge {
  background: rgba(255, 255, 255, 0.2);
  border: none;
  color: white;
  font-size: 10px;
  padding: 0 4px;
  height: 16px;
  line-height: 16px;
}

.node-body {
  padding: 6px 12px;
}

.node-info {
  font-size: 10px;
  opacity: 0.9;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  display: block;
}
</style>
