<template>
  <div class="node script-node" :class="{ selected, disabled: !data.config?.enabled }">
    <Handle type="target" :position="Position.Left" />
    <div class="node-header">
      <span class="node-icon">{{ data.icon }}</span>
      <span class="node-label">{{ data.name }}</span>
      <el-tag size="small" class="script-badge">{{ getScriptType() }}</el-tag>
    </div>
    <div class="node-body">
      <span class="node-info">{{ getPreview() }}</span>
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

const getScriptType = () => {
  if (props.data.type === 'script_python') return 'PY'
  if (props.data.type === 'script_js') return 'JS'
  if (props.data.type === 'script_http') return 'HTTP'
  if (props.data.type === 'script_json') return 'JSON'
  if (props.data.type === 'script_regex') return 'REGEX'
  return 'CODE'
}

const getPreview = () => {
  const code = props.data.config?.code || ''
  if (!code) return '点击配置...'
  const firstLine = code.split('\n')[0].trim()
  return firstLine.length > 20 ? firstLine.substring(0, 20) + '...' : firstLine
}
</script>

<style scoped>
.node {
  padding: 0;
  border-radius: 8px;
  min-width: 150px;
  background: linear-gradient(135deg, #4a6cf7 0%, #6b8cff 100%);
  color: white;
  box-shadow: 0 2px 8px rgba(74, 108, 247, 0.3);
  transition: all 0.2s;
}

.node.selected {
  box-shadow: 0 4px 16px rgba(74, 108, 247, 0.5);
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
  background: rgba(0, 0, 0, 0.15);
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

.script-badge {
  background: rgba(255, 255, 255, 0.25);
  border: none;
  color: white;
  font-size: 9px;
  padding: 0 4px;
  height: 14px;
  line-height: 14px;
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
