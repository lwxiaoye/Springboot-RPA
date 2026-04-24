// 全局 ECharts 配置和注册
import * as echarts from 'echarts/core'
import { LineChart, BarChart, PieChart, GaugeChart, ScatterChart, HeatmapChart } from 'echarts/charts'
import {
  GridComponent,
  TooltipComponent,
  LegendComponent,
  TitleComponent,
  MarkLineComponent,
  MarkPointComponent,
  DataZoomComponent,
  VisualMapComponent
} from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'

// 注册必要的组件
echarts.use([
  LineChart,
  BarChart,
  PieChart,
  GaugeChart,
  ScatterChart,
  HeatmapChart,
  GridComponent,
  TooltipComponent,
  LegendComponent,
  TitleComponent,
  MarkLineComponent,
  MarkPointComponent,
  DataZoomComponent,
  VisualMapComponent,
  CanvasRenderer
])

// 导出 echarts 实例供全局使用
export default echarts

// 设置全局主题配置
echarts.registerTheme('rpa-theme', {
  color: ['#409EFF', '#67C23A', '#E6A23C', '#F56C6C', '#909399', '#00B42A', '#FF7D00', '#165DFF'],
  backgroundColor: 'transparent',
  textStyle: {
    fontFamily: 'Microsoft YaHei, Arial, sans-serif'
  }
})
