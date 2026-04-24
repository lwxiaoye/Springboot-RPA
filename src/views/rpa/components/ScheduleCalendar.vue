<template>
  <div class="schedule-calendar">
    <div class="calendar-header">
      <el-button circle @click="prevMonth">
        <el-icon><ArrowLeft /></el-icon>
      </el-button>
      <span class="current-date">{{ currentYear }}年 {{ currentMonth + 1 }}月</span>
      <el-button circle @click="nextMonth">
        <el-icon><ArrowRight /></el-icon>
      </el-button>
    </div>
    
    <div class="calendar-weekdays">
      <div v-for="day in weekdays" :key="day" class="weekday">{{ day }}</div>
    </div>
    
    <div class="calendar-grid">
      <div
        v-for="(day, index) in calendarDays"
        :key="index"
        class="calendar-day"
        :class="{
          'other-month': day.otherMonth,
          'today': day.isToday,
          'has-schedule': day.hasSchedule,
          'selected': day.date === selectedDate
        }"
        @click="selectDate(day)"
      >
        <span class="day-number">{{ day.day }}</span>
        <div v-if="day.hasSchedule" class="schedule-dots">
          <span
            v-for="(schedule, i) in day.schedules.slice(0, 3)"
            :key="i"
            class="schedule-dot"
            :class="schedule.type"
          ></span>
        </div>
      </div>
    </div>
    
    <div class="schedule-list" v-if="selectedDate">
      <div class="list-header">
        <span>{{ selectedDate }} 的定时任务</span>
      </div>
      <div class="list-content">
        <div
          v-for="item in daySchedules"
          :key="item.id"
          class="schedule-item"
          :class="item.type"
        >
          <div class="item-time">{{ item.time }}</div>
          <div class="item-name">{{ item.name }}</div>
        </div>
        <div v-if="daySchedules.length === 0" class="no-schedule">
          暂无定时任务
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ArrowLeft, ArrowRight } from '@element-plus/icons-vue'

const props = defineProps({
  schedules: {
    type: Array,
    default: () => []
  }
})

const emit = defineEmits(['select'])

const weekdays = ['日', '一', '二', '三', '四', '五', '六']
const currentYear = ref(new Date().getFullYear())
const currentMonth = ref(new Date().getMonth())
const selectedDate = ref(null)

const calendarDays = computed(() => {
  const days = []
  const firstDay = new Date(currentYear.value, currentMonth.value, 1)
  const lastDay = new Date(currentYear.value, currentMonth.value + 1, 0)
  const startWeekday = firstDay.getDay()
  const totalDays = lastDay.getDate()
  const today = new Date()
  
  // 上个月的天数
  const prevMonthLastDay = new Date(currentYear.value, currentMonth.value, 0).getDate()
  
  // 添加上个月的日期
  for (let i = startWeekday - 1; i >= 0; i--) {
    const day = prevMonthLastDay - i
    const date = formatDate(currentYear.value, currentMonth.value - 1, day)
    days.push({
      day,
      date,
      otherMonth: true,
      isToday: false,
      hasSchedule: checkHasSchedule(date),
      schedules: getSchedulesByDate(date)
    })
  }
  
  // 当前月的日期
  for (let day = 1; day <= totalDays; day++) {
    const date = formatDate(currentYear.value, currentMonth.value, day)
    const isToday = today.getFullYear() === currentYear.value &&
                   today.getMonth() === currentMonth.value &&
                   today.getDate() === day
    days.push({
      day,
      date,
      otherMonth: false,
      isToday,
      hasSchedule: checkHasSchedule(date),
      schedules: getSchedulesByDate(date)
    })
  }
  
  // 添加下个月的日期，补齐到42天（6行）
  const remaining = 42 - days.length
  for (let day = 1; day <= remaining; day++) {
    const date = formatDate(currentYear.value, currentMonth.value + 1, day)
    days.push({
      day,
      date,
      otherMonth: true,
      isToday: false,
      hasSchedule: checkHasSchedule(date),
      schedules: getSchedulesByDate(date)
    })
  }
  
  return days
})

const daySchedules = computed(() => {
  if (!selectedDate.value) return []
  return getSchedulesByDate(selectedDate.value)
})

function formatDate(year, month, day) {
  const d = new Date(year, month, day)
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
}

function checkHasSchedule(date) {
  return props.schedules.some(s => s.date === date)
}

function getSchedulesByDate(date) {
  return props.schedules.filter(s => s.date === date)
}

function prevMonth() {
  if (currentMonth.value === 0) {
    currentMonth.value = 11
    currentYear.value--
  } else {
    currentMonth.value--
  }
}

function nextMonth() {
  if (currentMonth.value === 11) {
    currentMonth.value = 0
    currentYear.value++
  } else {
    currentMonth.value++
  }
}

function selectDate(day) {
  if (day.otherMonth) return
  selectedDate.value = day.date
  emit('select', day)
}

onMounted(() => {
  // 设置今天为默认选中
  const today = new Date()
  selectedDate.value = formatDate(today.getFullYear(), today.getMonth(), today.getDate())
})
</script>

<style scoped>
.schedule-calendar {
  background: #fff;
  border-radius: 8px;
  padding: 16px;
}

.calendar-header {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 16px;
  margin-bottom: 16px;
}

.current-date {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  min-width: 120px;
  text-align: center;
}

.calendar-weekdays {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  gap: 4px;
  margin-bottom: 8px;
}

.weekday {
  text-align: center;
  font-size: 12px;
  color: #909399;
  padding: 8px 0;
}

.calendar-grid {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  gap: 4px;
}

.calendar-day {
  aspect-ratio: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
  position: relative;
  padding: 4px;
}

.calendar-day:hover:not(.other-month) {
  background: #f5f7fa;
}

.calendar-day.other-month {
  opacity: 0.4;
  cursor: default;
}

.calendar-day.today {
  background: #ecf5ff;
}

.calendar-day.today .day-number {
  color: #409eff;
  font-weight: 600;
}

.calendar-day.selected {
  background: #409eff;
}

.calendar-day.selected .day-number {
  color: #fff;
}

.day-number {
  font-size: 14px;
  color: #303133;
}

.schedule-dots {
  display: flex;
  gap: 2px;
  margin-top: 2px;
}

.schedule-dot {
  width: 4px;
  height: 4px;
  border-radius: 50%;
  background: #409eff;
}

.schedule-dot.daily {
  background: #67c23a;
}

.schedule-dot.weekly {
  background: #e6a23c;
}

.schedule-dot.monthly {
  background: #f56c6c;
}

.schedule-list {
  margin-top: 16px;
  border-top: 1px solid #ebeef5;
  padding-top: 16px;
}

.list-header {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 12px;
}

.schedule-item {
  display: flex;
  gap: 12px;
  padding: 10px;
  background: #f5f7fa;
  border-radius: 6px;
  margin-bottom: 8px;
  border-left: 3px solid #409eff;
}

.schedule-item.daily {
  border-left-color: #67c23a;
}

.schedule-item.weekly {
  border-left-color: #e6a23c;
}

.item-time {
  font-size: 13px;
  color: #606266;
  min-width: 50px;
}

.item-name {
  font-size: 13px;
  color: #303133;
}

.no-schedule {
  text-align: center;
  color: #909399;
  padding: 20px;
  font-size: 13px;
}
</style>
