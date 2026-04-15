<script setup>
import { onBeforeUnmount, onMounted, ref, watch } from 'vue'
import Chart from 'chart.js/auto'

const props = defineProps({
  points: {
    type: Array,
    default: () => [],
  },
})

const canvasRef = ref(null)
let chartInstance = null

function buildChart() {
  if (!canvasRef.value) {
    return
  }

  const scatterPoints = props.points
  const scatterDataset = scatterPoints.map((point) => ({
    x: point.x,
    y: point.y,
  }))

  const pointColors = scatterPoints.map((point) => {
    if (point.pass === false) {
      return '#d32f2f'
    }
    if (point.pass === true) {
      return '#2e7d32'
    }
    return '#757575'
  })

  const maxX = scatterPoints.length > 0 ? Math.max(...scatterPoints.map((point) => point.x), 0) : 0
  const lineMaxX = maxX < 1 ? 1 : maxX

  const datasets = [
    {
      type: 'scatter',
      label: 'color_de (로트 순번 대비)',
      data: scatterDataset,
      pointBackgroundColor: pointColors,
      pointRadius: 6,
    },
  ]

  if (scatterPoints.length > 0) {
    datasets.push({
      type: 'line',
      label: '기준 DE 1.0',
      data: [
        { x: 0, y: 1 },
        { x: lineMaxX, y: 1 },
      ],
      borderColor: '#ed6c02',
      borderDash: [6, 4],
      pointRadius: 0,
      fill: false,
      tension: 0,
    })
  }

  if (chartInstance) {
    chartInstance.data.datasets = datasets
    chartInstance.update('none')
    return
  }

  chartInstance = new Chart(canvasRef.value, {
    type: 'scatter',
    data: { datasets },
    options: {
      responsive: true,
      maintainAspectRatio: false,
      plugins: {
        tooltip: {
          filter: (tooltipItem) => tooltipItem.datasetIndex === 0,
          callbacks: {
            label(context) {
              const pointIndex = context.dataIndex
              const sourcePoint = scatterPoints[pointIndex]
              if (!sourcePoint) {
                return ''
              }
              return `wo_id ${sourcePoint.label}: color_de ${sourcePoint.y}`
            },
          },
        },
        legend: {
          display: true,
        },
      },
      scales: {
        x: {
          type: 'linear',
          title: {
            display: true,
            text: '로트 순번 (목록 순서)',
          },
          min: 0,
        },
        y: {
          title: {
            display: true,
            text: 'color_de',
          },
        },
      },
    },
  })
}

onMounted(() => {
  buildChart()
})

watch(
  () => props.points,
  () => {
    buildChart()
  },
  { deep: true },
)

onBeforeUnmount(() => {
  if (chartInstance) {
    chartInstance.destroy()
    chartInstance = null
  }
})
</script>

<template>
  <div class="color-de-scatter-chart">
    <canvas ref="canvasRef" />
  </div>
</template>

<style scoped>
.color-de-scatter-chart {
  position: relative;
  height: 300px;
}
</style>
